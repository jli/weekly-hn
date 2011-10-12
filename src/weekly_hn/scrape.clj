(ns weekly-hn.scrape
  (:use [net.cgrand.enlive-html :only [html-resource text select]])
  (:import [java.util Calendar Date]))


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
(defn get-web [] (safe (html-resource hn-rss) nil))

(defn get-raw-file [f] (-> f java.io.File. html-resource))
(defn get-file [f] (-> f java.io.File. slurp read-string))



;;; parsing, munging

;; stories look like:
;;   Node.js has jumped the shark (unlimitednovelty.com)
;;   289 points by bascule 10 hours ago | 104 comments
;; some stories are missing some info:
;;   Mixpanel is hiring: Here is where we're at.
;;   4 hours ago
;; not sure what the deal is. just YC hiring? let's just drop those.

(defn remove-precision! [cal unit]
  (let [immed {Calendar/SECOND Calendar/MILLISECOND
               Calendar/MINUTE Calendar/SECOND
               Calendar/HOUR_OF_DAY Calendar/MINUTE
               Calendar/DAY_OF_MONTH Calendar/HOUR_OF_DAY}
        lessers (fn [unit]
                  (take-while (comp not nil?)
                              (iterate immed
                                       (immed unit))))]
    (doseq [smaller-unit (lessers unit)]
      (.set cal smaller-unit 0))
    cal))

;; times get more imprecise over time (minute -> hour -> day)
;; 1. reflect this by rounding back
;; 2. preserve the time of the original story when merging (see
;;    update-work-set below)
(defn parse-time [s]
  (let [cal-unit {"minute" Calendar/MINUTE
                  "hour" Calendar/HOUR_OF_DAY ; _OF_DAY matters?
                  "day" Calendar/DAY_OF_MONTH}
        cal (Calendar/getInstance)
        [_ n unit] (re-matches #"^ *([0-9]+) (minute|hour|day)s? ago.*$" s)
        n (Integer. n)]
    ;; mutate cal
    (.add cal (cal-unit unit) (- n))
    (remove-precision! cal (cal-unit unit))
    ;; cal -> date -> epoch ms
    (.getTime (.getTime cal))))

;; what a mess!
(defn parse-story [title subtext]
  (let [[a] (:content title)
        link (get-in a [:attrs :href])
        title (text a)
        [pointspan _by usera time commenta] (:content subtext)
        id (-> (get-in pointspan [:attrs :id]) ;; "score_13289"
               (.split "_") second Integer.)
        points (->> (text pointspan)
                    (re-matches #"([0-9]+) point.*")
                    second Integer.)
        user (text usera)
        time (safe (parse-time-exn time) (Date.))
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
     :time time
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
        parses (map (fn [[link sub]]
                      (safe (parse-story link sub) [:fail link sub]))
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
  ;; keep time from older fetch, because new time values will be less
  ;; precise (see parse-story, parse-time above)
  (let [keep-time (fn [old new]
                    (if-let [time (:time old)]
                      (assoc new :time time)
                      new))]
    (merge-with keep-time work-set (index-stories stories))))

;; cut issue from current work-set!
(defn work-set->issue [work-set]
  {:date (.getTime (Date.))
   :stories (vals work-set)})

;; filters current work-set based on past archives
;; currently, removes everything in last issue.
;; not great. something based on scores or growth instead?
(defn work-set-filter-new [archive work-set]
  (let [issue-ids #(map :id (:stories %))
        prev-ids (set (issue-ids (first archive)))]
    (filter (fn [[id _]] (not (prev-ids id)))
            work-set)))



;;; serving interface, refs, files and junk

;; todo horribly boiler-platey

(defn archive-file [log-dir] (str log-dir "/archive.sexp"))
(defn work-set-file [log-dir] (str log-dir "/work-set.sexp"))

(defn load-archive [log-dir]
  (-> (archive-file log-dir) slurp read-string))
(defn load-work-set [log-dir]
  (-> (work-set-file log-dir) slurp read-string))

(defonce issue-archive (atom '()))
(defonce work-set (atom {}))

(defn backup-archive [log-dir msg-pre]
  (try-log (spit (archive-file log-dir) (prn-str @issue-archive))
           (str msg-pre ": backing up archive file sucked")))
(defn backup-work-set [log-dir msg-pre]
  (try-log (spit (work-set-file log-dir) @work-set)
           (str msg-pre ": work-set failwhale")))

;; run once at start-up
(defn reload-data [log-dir]
  (reset! issue-archive (safe (load-archive log-dir) '()))
  (reset! work-set (safe (load-work-set log-dir) {}))
  (println "issue-archive:")
  (doseq [{d :date} @issue-archive] (println " " d))
  (println "work-set:" (sort (keys @work-set))))

;; external interface

(defn issue-in-progress []
  (vals (work-set-filter-new @issue-archive @work-set)))

(defn archive-index []
  (map :date @issue-archive))

(defn issue->stories [date]
  (let [[issue] (filter #(= date (:date %)) @issue-archive)]
    (:stories issue)))



;;; update loops

;; todo think about concurrency

(defn fetch-and-update! [log-dir]
  (dosync
   (let [date (Date.)
         file-path (fn [type]
                     (format "%s/fetch/%s.%s"
                             log-dir (format-date date) (name type)))
         raw-file (file-path :raw)
         stories-file (file-path :stories)
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

(defn cut-issue! [log-dir]
  (dosync
   (let [work-set-new (work-set-filter-new @issue-archive @work-set)
         new-issue (work-set->issue work-set-new)]
     (swap! issue-archive conj new-issue)
     (reset! work-set {})
     (backup-archive log-dir "cut-issue!")
     (backup-work-set log-dir "cut-issue!"))))

;; blah, really want to say "every week", "every 3 hours", etc.
(defn do-every [wait now? f name ]
  (let [tfn (fn []
              (when-not now?
                (println "not now - waiting for" wait)
                (Thread/sleep wait))
              (loop []
                (println (Date.) name "> doing")
                (f)
                (Thread/sleep wait)
                (recur)))]
   (doto (Thread. tfn name) .start)))

;; debugging
(def work-set-thread (atom nil))
(def issue-thread (atom nil))

(defn work-set-updater [log-dir minutes]
  (reset! work-set-thread
          (do-every (* 1000 60 minutes) true
                    #(fetch-and-update! log-dir)
                    "work-set updater")))

(defn issue-cutter [log-dir]
  (reset! issue-thread
          (do-every (* 1000 3600 24 7) false
                    #(cut-issue! log-dir)
                    "issue cutter")))
