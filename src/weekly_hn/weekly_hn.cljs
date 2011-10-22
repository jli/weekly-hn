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

(defn index-with [f xs]
  (reduce (fn [ac x] (assoc ac (f x) x)) {} xs))

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
(defn maybe-hn [link]
  (href
   (if (string/startsWith link "item?id=")
     (str "https://news.ycombinator.com/" link)
     link)))

(defn render-story [{:keys [id link title points user comments time]}]
  (let [pnode (node "a" (.strobj {"class" "points"
                                  "href" (hn-link id)})
                    (str points))
        a (node "a" (maybe-hn link) title)]
    (node "span" nil pnode " " a " " (render-site link) (render-post-time time))))

(defn render-story-list [stories]
  (apply node "ol" (id "storylist")
         (map (fn [s] (node "li" nil (render-story s))) stories)))

(defn render-limiter [start total step]
  (let [limits (snoc (range start total step) total)
        opts (map #(node "option" (.strobj {"value" %}) (str %)) limits)]
     (apply node "select" (class "limiter") opts)))



;;; thundercats are go

;; 50, hard limit. don't forget to live.
(def spectatoritis-is-a-cancer 50)

(def issue-cache (atom {}))

;; nil limit means unlimited
(defn with-issue
  ([date-or-wip f] (with-issue date-or-wip f spectatoritis-is-a-cancer))
  ([date-or-wip f limit]
     (let [url (if (nil? limit)
                 (if (= date-or-wip :wip)
                   "/wip"
                   (str "/issue?d=" date-or-wip))
                 (if (= date-or-wip :wip)
                   (str "/wip-take?n=" limit)
                   (str "/issue-take?d=" date-or-wip "&n=" limit)))
           fetch #(Xhr/send url (fn [e]
                                  (let [issue (event->clj e)]
                                    (f issue)
                                    (swap! issue-cache conj [date-or-wip [issue limit]]))))]
       (if-let [[issue issue-limit] (get @issue-cache date-or-wip)]
         (cond (= limit issue-limit) (f issue)
               ;; cached issue has everything
               (or (nil? issue-limit)
                   ;; or current request is less than what we've got
                   (and (not (nil? limit))
                        (> issue-limit limit))) (f (take limit issue))
               :default (fetch))
         (fetch)))))

(defn update-listing [limiter stories]
  (let [stories (take (.value limiter) stories)]
    (dom/replaceNode (render-story-list stories)
                     (dom/getElement "storylist"))))

;; mess. set-issue should just build and render. history pushing,
;; event.preventDefault, etc. should happen elsewhere.
(defn set-issue [date-or-wip push-state? stories]
  (let [title (if (= :wip date-or-wip) "issue in-progress" (render-date date-or-wip))
        url (if (= :wip date-or-wip) "iip" (render-date date-or-wip))
        h2 (node "h2" nil title)
        listing (render-story-list stories)
        flood (node "button" (class "flood") "terrible flood")
        head (node "div" (class "head") h2 flood)
        issue (node "div" nil head listing)]
    ;; state is simply date-or-wip, used to call load-issue again
    (when (and push-state?
               (.pushState window.history))
      (.pushState window.history date-or-wip "" (str "/" url)))
    (events/listen flood events/EventType.CLICK
                   (fn [_]
                     (with-issue date-or-wip (partial set-issue date-or-wip false) nil)))
    (dom/removeChildren (dom/getElement "issue"))
    (dom/insertChildAt (dom/getElement "issue") issue)))

(defn load-issue
  ([date-or-wip] (load-issue date-or-wip true))
  ([date-or-wip push-state?]
     (with-issue date-or-wip (partial set-issue date-or-wip push-state?))))

(defn load-issue-handler [date-or-wip event]
  (. event (preventDefault))
  (load-issue date-or-wip))

(defn set-index [dates]
  (let [wip (doto (node "a" (href "iip") "issue in-progress")
              (events/listen events/EventType.CLICK #(load-issue-handler :wip %)))
        items (map (fn [d]
                     (let [date (render-date d)
                           link (node "a" (href date) date)]
                       (events/listen link events/EventType.CLICK #(load-issue-handler d %))
                       (node "span" nil link)))
                   dates)
        all (apply node "span" nil wip " " (interpose " " items))]
    (dom/insertChildAt (dom/getElement "index") all)))

(defn load-index [k]
  (Xhr/send "/index" (fn [e]
                       (let [in (event->clj e)]
                         (set-index in)
                         (k in)))))

(defn ^:export rock-and-roll []
  (let [loc (.replace window.location.pathname #"^/" "")
        index-k (fn [index]
                  (let [date->ms (index-with render-date index)]
                    (cond
                     (= loc "iip") (load-issue :wip)
                     :default (if-let [date-ms (date->ms loc)]
                                (load-issue date-ms)
                                (if-let [latest (first index)]
                                  (load-issue latest)
                                  (load-issue :wip))))))
        popstate (fn [e] (load-issue (.state e) false))]
    (when (js* "window['onpopstate']")
      (js* "window['onpopstate'] = ~{popstate}"))
    (load-index index-k)))
