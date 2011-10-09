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
(defn href [url] (.strobj {"href" url}))

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

(defn format-date [ms]
  (.toIsoString (ms->date ms) true))

;; todo drop time
(defn render-date [ms]
  (format-date ms))

(defn render-site [url]
  ;; why "\." not "\\."?
  (let [url (-> url
                (.replace #"^https?://(www\.)?" "")
                (.replace #"/.+$" ""))]
    (node "span" (class "site") url)))

(defn hn-link [id]
  (str "https://news.ycombinator.com/item?id=" id))

(defn render-story [{:keys [id link title points user comments]}]
  (let [pnode (node "a" (.strobj {"class" "points"
                                  "href" (hn-link id)})
                    (str points))
        a (node "a" (href link) title)]
    (node "span" nil pnode " " a " " (render-site link))))



;;; thundercats are go

(defn set-issue [title stories]
  (let [head (node "h2" nil title)
        snodes (map (fn [s] (node "li" nil (render-story s)))
                    (sort-by :points > stories))
        listing (apply node "ol" nil snodes)
        issue (node "div" nil head listing)]
    (dom/removeChildren (dom/getElement "issue"))
    (dom/insertChildAt (dom/getElement "issue") issue)))

(defn load-issue [date]
  (Xhr/send (str "/issue?d=" date)
            (fn [e] (set-issue (render-date date) (event->clj e)))))

(defn load-wip []
  (Xhr/send (str "/wip")
            (fn [e] (set-issue "issue in-progress" (event->clj e)))))

(defn set-index [dates]
  (let [wip (node "a" (.strobj {"href" "#"}) "in-progress issue")
        items (map (fn [d]
                     (let [link (node "a" (.strobj {"href" "#"}) (render-date d))]
                       (events/listen link events/EventType.CLICK #(load-issue d))
                       (node "span" nil link)))
                   dates)
        all (apply node "span" nil wip items)]
    (events/listen wip events/EventType.CLICK #(load-wip))
    (dom/insertChildAt (dom/getElement "index") all)))

(defn ^:export kickoff []
  (Xhr/send "/index"
            (fn [e]
              (let [index (event->clj e)]
                (if-let [latest (first index)]
                  (load-issue latest)
                  (load-wip))
                (set-index index)))))
