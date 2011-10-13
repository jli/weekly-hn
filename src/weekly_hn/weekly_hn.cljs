(ns weekly_hn.main
  (:require [goog.dom :as dom]
            ;; [goog.string :as string]
            ;; [goog.array :as array]
            [goog.net.XhrIo :as Xhr]
            [goog.events.EventType :as EventType]
            [goog.Timer :as Timer]
            [goog.events :as events]
            [goog.date :as date]
            [goog.string :as string]
            [cljs.reader :as reader]))

;;; utils

(def html dom/htmlToDocumentFragment)
(def node dom/createDom)
(defn class [s] (.strobj {"class" s}))
(defn id [s] (.strobj {"id" s}))
(defn href [url] (.strobj {"href" url}))

(defn snoc [xs x] (concat xs [x]))

(defn js-alert [& args]
  (let [msg (apply str args)]
    (js* "alert(~{msg})")))

(def db (dom/getElement "dbug"))
(defn dbg [& args]
  (dom/insertChildAt db (html (apply str "<br>" (goog.date.DateTime.) "|" args))))

(defn ms->date [ms]
  (doto (goog.date.Date.) (. (setTime ms))))

(defn ms->datetime [ms]
  (doto (goog.date.DateTime.) (. (setTime ms))))

(defn uri-opts [m]
  (let [pairs (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v))) m)]
    (apply str (interpose "&" pairs))))

(defn event->clj [e]
   (-> (.target e)
       (. (getResponseText))
       reader/read-string))



;;; stuff

(defn render-date [ms] (.toIsoString (ms->date ms) true))

;; oh god
(defn render-post-time [ms]
  (when ms
    (let [pad #(string/padNumber % 2)
          dt (ms->datetime ms)
          mon (inc (. dt (getMonth)))
          dayom (. dt (getDate))
          hr    (. dt (getHours))
          min   (. dt (getMinutes))
          hrs  (when-not (zero? hr) (str " " (pad hr)))
          mins (when-not (zero? min) (str ":" (pad min)))]
      (node "span" (class "posttime") " "
            (pad mon) "/" (pad dayom)
            hrs mins))))

(defn render-site [url]
   ;; hm, not sure why "\." not "\\."
  (let [base (nth (.exec #"^https?://(?:www\.)?([^/]+)" url) 1)]
    (node "span" (class "site") base)))

(defn hn-link [id] (str "https://news.ycombinator.com/item?id=" id))

(defn render-story [{:keys [id link title points user comments time]}]
  (let [pnode (node "a" (.strobj {"class" "points"
                                  "href" (hn-link id)})
                    (str points))
        a (node "a" (href link) title)]
    (node "span" nil pnode " " a " " (render-site link) (render-post-time time))))

(defn render-story-list [stories]
  (apply node "ol" (id "storylist")
         (map (fn [s] (node "li" nil (render-story s))) stories)))

(defn render-limiter [start total step]
  (let [;; filter total because total would be same as "all" added later
        limits (filter (partial not= total) (range start total step))
        opts (snoc (map #(node "option" (.strobj {"value" %}) (str %)) limits)
                   (node "option" (.strobj {"value" total}) "all"))]
     (apply node "select" (class "limiter") opts)))



;;; thundercats are go

(def issue-cache (atom {}))

(defn with-issue [date f]
  (if-let [issue (get @issue-cache date)]
    (f issue)
    (Xhr/send (str "/issue?d=" date)
              (fn [e]
                (let [issue (event->clj e)]
                  (f issue)
                  (swap! issue-cache conj [date issue]))))))

(defn update-listing [limiter stories]
  (let [stories (take (.value limiter) stories)]
    (dom/replaceNode (render-story-list stories)
                     (dom/getElement "storylist"))))

(defn set-issue [title stories]
  (let [h2 (node "h2" nil title)
        init-limit 10
        limiter (render-limiter init-limit (count stories) 10)
        listing (render-story-list (take init-limit stories))
        head (node "div" (class "head") h2 " " limiter)
        issue (node "div" nil head listing)]
    (events/listen limiter events/EventType.CHANGE #(update-listing limiter stories))
    (dom/removeChildren (dom/getElement "issue"))
    (dom/insertChildAt (dom/getElement "issue") issue)))

(defn load-issue [date]
  (with-issue date
    (fn [issue] (set-issue (render-date date) issue))))

(defn load-wip []
  (Xhr/send (str "/wip") #(do ;; (dbg "loaded. reading")
                              (let [c (event->clj %)]
                                ;; (dbg "read.")
                                (set-issue "issue in-progress" c)))))

(defn set-index [dates]
  (let [wip (doto (node "a" (href "#") "issue in-progress")
              (events/listen events/EventType.CLICK #(load-wip)))
        items (map (fn [d]
                     (let [link (node "a" (href "#") (render-date d))]
                       (events/listen link events/EventType.CLICK #(load-issue d))
                       (node "span" nil link)))
                   dates)
        all (apply node "span" nil wip " " (interpose " " items))]
    (dom/insertChildAt (dom/getElement "index") all)))

(defn ^:export kickoff []
  (Xhr/send "/index"
            (fn [e]
              (let [index (event->clj e)]
                (if-let [latest (first index)]
                  (load-issue latest)
                  (load-wip))
                (set-index index)))))
