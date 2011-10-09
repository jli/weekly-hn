(ns weekly-hn.scrape
  (:use [net.cgrand.enlive-html :only [html-resource text select]]))


;;; util

(defmacro safe
  ;; todo doesn't work like I think it does!!!
  ;; ([exp] (safe exp nil))
  ([exp val]
     (let [e (gensym)]
       `(try ~exp (catch Exception ~e ~val)))))

(defmacro try-log [exp msg]
  (let [e (gensym)]
    `(try ~exp (catch Exception ~e (println ~msg ":->" ~e)))))

(defn split-with-all [p? xs]
  [(filter p? xs)
   (filter (comp not p?) xs)])

(defn index-with [f xs]
  (reduce (fn [ac x] (assoc ac (f x) x)) {} xs))

(def formatter (java.text.SimpleDateFormat. "yyyy-MM-dd_HH:mm:ss"))
(defn format-date [date] (.format formatter date))

(def hn-rss (java.net.URL. "http://news.ycombinator.com"))

;; parsed into nodes
(defn get-web [] (html-resource hn-rss))
(defn get-file [f] (-> f java.io.File. html-resource))



;;; parsing, munging

;; stories look like:
;;   Node.js has jumped the shark (unlimitednovelty.com)
;;   289 points by bascule 10 hours ago | 104 comments
;; some stories are missing some info:
;;   Mixpanel is hiring: Here is where we're at.
;;   4 hours ago
;; not sure what the deal is. let's just drop those.

;; what a mess!
;; todo record
(defn transform-story [title subtext]
  (let [[a] (:content title)
        link (get-in a [:attrs :href])
        title (text a)
        [pointspan _by usera _time commenta] (:content subtext)
        id (-> (get-in pointspan [:attrs :id]) ;; "score_13289"
               (.split "_") second Integer.)
        points (->> (text pointspan)
                    (re-matches #"([0-9]+) point.*")
                    second Integer.)
        user (text usera)
        ;; can be "discuss" or something
        parse-int (fn [s] (safe (Integer. s) 0))
        comments (->> (text commenta)
                      (re-matches #"([0-9]+) comment.*")
                      second
                      parse-int)]
    {:id id
     :link link
     :title title
     :points points
     :user user
     :comments comments}))

;; links and other data (points, user, comments, etc.) are not grouped :(
(defn group-story-nodes [node]
  (let [links-subs (select node #{[:td.title] [:td.subtext]})
        class? (fn [node class] (= class (get-in node [:attrs :class])))]
    (filter (fn [[link sub]] (and (class? link "title")
                                  (class? sub "subtext")))
            (partition 2 1 links-subs))))

;; nodes -> stories coll
(defn stories [node]
  (let [grouped (group-story-nodes node)
        parses (map (fn [[link sub]] (safe (transform-story link sub) [:fail link sub]))
                    grouped)
        [fails stories] (split-with-all #(= :fail (first %)) parses)]
    ;; todo logging
    (when-not (empty? fails) (println "failed:" fails))
    stories))



;;; issues, archives, work-set stuff

;; work set type: id,story map
;; issue type: {:date <j.u.date>, :stories <story coll>}
;; archive type: issue list

(defn index-stories [stories] (index-with :id stories))

(defn update-work-set [work-set stories]
  (merge work-set (index-stories stories)))

(defn work-set->issue [work-set]
  {:date (java.util.Date.)
   :stories (vals work-set)})

(defn issue-ids [issue] (map :id (:stories issue)))

(defonce issue-archive (atom '()))
(defonce work-set (atom {}))

(defn latest-issue [] (first @issue-archive))
(defn latest-stories [] (:stories (latest-issue)))

(defn archive-index []
  (map (comp (memfn getTime) :date) @issue-archive))

(defn get-stories [date]
  (let [[issue] (filter #(= date (:date %)) @issue-archive)]
    (:stories issue)))

;; todo abstract better
(defn backup-archive [log-dir msg-pre]
  (let [arc (map (fn [{:keys [date stories]}]
                   {:date (.getTime date) :stories stories})
                 @issue-archive)]
    (try-log (spit (str log-dir "/archive.sexp") (prn-str arc))
             (str msg-pre ": backing up archive file sucked"))))

(defn load-archive [log-dir]
  (->> (str log-dir "/archive.sexp")
       slurp
       read-string
       (map (fn [{:keys [date stories]}]
              {:date (java.util.Date. date)
               :stories stories}))))

(defn backup-work-set [log-dir msg-pre]
  (try-log (spit (str log-dir "/work-set.sexp") @work-set)
           (str msg-pre ": saving work-set")))

(defn file-path [type base-dir date]
  (str base-dir "/" (format-date date) "." (name type)))

;; todo needs dosync?
(defn fetch-and-update! [log-dir]
  (dosync
   (let [date (java.util.Date.)
         raw-file (file-path :raw log-dir date)
         stories-file (file-path :stories log-dir date)
         raw (get-web)
         stories (stories raw)
         prev-count (count @work-set)]
     (swap! work-set update-work-set stories)
     (println "updated working set." prev-count "->" (count @work-set))
     (backup-work-set log-dir "fetch-and-update!")
     (try-log (spit raw-file (prn-str raw))
              "fetch-and-update!: saving raw was weird")
     (try-log (spit stories-file (prn-str stories))
              "fetch-and-update!: saving stories was weird"))))

;; todo needs dosync?
(defn cut-issue! [log-dir]
  (dosync
   (let [prev-ids (set (issue-ids (latest-issue)))
         work-set-all @work-set
         work-set-new (filter (fn [[id _]] (not (prev-ids id))) work-set-all)
         new-issue (work-set->issue work-set-new)]
     (swap! issue-archive conj new-issue)
     (reset! work-set {})
     (backup-archive log-dir "cut-issue!")
     (backup-work-set log-dir "cut-issue!"))))

(defn work-set-updater-loop [wait log-dir]
  (-> (loop []
        (fetch-and-update! log-dir)
        (Thread/sleep wait)
        (recur))
      Thread.
      .start))
