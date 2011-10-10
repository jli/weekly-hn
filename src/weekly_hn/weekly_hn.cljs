(ns weekly_hn.main
  (:require [goog.dom :as dom]
            ;; [goog.string :as string]
            ;; [goog.array :as array]
            [goog.net.XhrIo :as Xhr]
            [goog.events.EventType :as EventType]
            [goog.events :as events]
            [goog.date :as date]
            [goog.Timer :as Timer]
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

(defn ms->date [ms]
  (doto (goog.date.Date.) (. (setTime ms))))

(defn uri-opts [m]
  (let [pairs (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v))) m)]
    (apply str (interpose "&" pairs))))

(defn event->clj [e]
   (-> (.target e)
       (. (getResponseText))
       reader/read-string))



;;; stuff

(defn render-date [ms] (.toIsoString (ms->date ms) true))

(defn render-site [url]
   ;; hm, not sure why "\." not "\\."
  (let [base (nth (.exec #"^https?://(?:www\.)?([^/]+)" url) 1)]
    (node "span" (class "site") base)))

(defn hn-link [id] (str "https://news.ycombinator.com/item?id=" id))

(defn render-story [{:keys [id link title points user comments]}]
  (let [pnode (node "a" (.strobj {"class" "points"
                                  "href" (hn-link id)})
                    (str points))
        a (node "a" (href link) title)]
    (node "span" nil pnode " " a " " (render-site link))))

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
                  (swap! issue-cache conj [date issue])
                  (f issue))))))

(defn update-listing [limiter stories]
  (let [stories (take (.value limiter) stories)]
    (dom/replaceNode (render-story-list stories)
                     (dom/getElement "storylist"))))

(defn set-issue [title raw-stories]
  (let [stories (sort-by :points > raw-stories)
        h2 (node "h2" nil title)
        init-limit 10
        limiter (render-limiter init-limit  (count stories) 10)
        listing (render-story-list (take init-limit stories))
        head (node "div" (class "head") h2 " " limiter)
        issue (node "div" nil head listing)]
    (events/listen limiter events/EventType.CHANGE #(update-listing limiter stories))
    (dom/removeChildren (dom/getElement "issue"))
    (dom/insertChildAt (dom/getElement "issue") issue)))

(defn load-issue [date]
  ;; (Xhr/send (str "/issue?d=" date) #(set-issue (render-date date) (event->clj %)))
  (with-issue date
    (fn [issue] (set-issue (render-date date) issue))))

(defn load-wip []
  (Xhr/send (str "/wip") #(set-issue "issue in-progress" (event->clj %))))

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
