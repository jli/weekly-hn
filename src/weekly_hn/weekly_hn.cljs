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

(defn render-story [{:keys [id link title points user comments]}]
  (let [a (node "a" (.strobj {"href" link}) title)]
    (node "span" nil (str points) " " a)))



;;; thundercats are go

(defn set-issue [date stories]
  (let [date (node "h2" nil (render-date date))
        snodes (map (fn [s]
                      (node "div" nil (render-story s)))
                    (sort-by :points > stories))
        issue (apply node "div" nil date snodes)]
    (dom/insertChildAt (dom/getElement "issue") issue)))

(defn load-issue [date]
  (let [k (fn [e] (set-issue date (event->clj e)))]
    (Xhr/send (str "/issue?d=" date) k)))

(defn set-index [dates]
  (let [items (map (fn [d]
                     (let [link (node "a" (.strobj {"href" "#"}) (render-date d))]
                       (events/listen link events/EventType.CLICK #(load-issue d))
                       (node "li" nil link)))
                   dates)
        list (apply node "ul" nil items)]
    (dom/insertChildAt (dom/getElement "index") list)))

(defn ^:export kickoff []
  (Xhr/send "/index" (fn [e]
                       (let [index (event->clj e)]
                         (if-let [latest (first index)]
                           (load-issue latest))
                         (set-index index)))))
