(ns weekly-hn.scrape
  (:use [net.cgrand.enlive-html :only [html-resource text select]]))

;;; util

(defmacro safe
  ([f] (safe f nil))
  ([f val]
     (let [e (gensym)]
      `(try ~f (catch Exception ~e ~val)))))

(defn split-with-all [p? xs]
  [(filter p? xs)
   (filter (comp not p?) xs)])

(def hn-rss (java.net.URL. "http://news.ycombinator.com"))

(defn get-current [] (html-resource hn-rss))

(defn get-file [f] (-> f java.io.File. html-resource))

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
                    second)
        user (text usera)
        comments (->> (text commenta)
                      (re-matches #"([0-9]+) comment.*")
                      second
                      Integer.)]
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

(defn stories [node]
  (let [grouped (group-story-nodes node)
        parses (map (fn [[link sub]] (safe (transform-story link sub) [:fail link sub]))
                    grouped)
        [fails stories] (split-with-all #(= :fail (first %)) parses)]
    ;; todo logging
    (println "failed:" fails)
    stories))
