(ns weekly-hn.core
  (:use [compojure.core]
        [compojure.route :only [resources]]
        [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [response file-response]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.stacktrace :only [wrap-stacktrace]]
        [ring.middleware.gzip :only [wrap-gzip]]
        [hiccup.core :only [html]]
        [clojure.tools.cli :only [cli optional]])
  (:require [swank.swank]
            [weekly-hn.scrape :as scrape]
            [weekly-hn.site :as site])
  (:gen-class))

(defn date-page [date-str full?]
  (if-let [stories (scrape/issue->stories date-str)]
    (html (site/issue-page date-str date-str
                           stories (scrape/archive-index) full?))))

(defn iip-page [full?]
  (html (site/issue-page "issue in-progress" "iip"
                         (scrape/issue-in-progress)
                         (scrape/archive-index)
                         full?)))

(defn rationale-page [] (html (site/rationale-page (scrape/archive-index))))

(defn index-page []
  (let [index (scrape/archive-index)]
    (if-let [latest (first index)]
      (date-page latest false)
      (iip-page false))))

(defroutes base
  (GET "/love" [] (response "<3"))
  (GET ["/:date" :date #"[0-9]{4}-[0-9]{2}-[0-9]{2}"] [date]
       (response (date-page date false)))
  (GET ["/:date.full" :date #"[0-9]{4}-[0-9]{2}-[0-9]{2}"] [date]
       (response (date-page date true)))
  (GET ["/iip"] [] (response (iip-page false)))
  (GET ["/iip.full"] [] (response (iip-page true)))
  (GET ["/rationale"] [] (response (rationale-page)))
  (GET "/" [] (response (index-page)))
  (resources "/")
  (ANY "*" [] (response (index-page))))

(def app
     (-> base
         wrap-params
         wrap-gzip
         wrap-stacktrace))

(defn -main [& args]
  (let [opts (cli args
                  (optional ["-j" "--jetty-port" :default 8080] #(Integer. %))
                  (optional ["-s" "--swank-port" :default 8081] #(Integer. %))
                  (optional ["-ns" "--no-swank" :default false])
                  (optional ["-d" "--data-dir" :default "store"])
                  (optional ["-w" "--wait" "how long between fetches (minutes)"
                             :default 30.] #(Float. %))
                  (optional ["-n" "--now" "cut issue now?" :default false]))]
    (let [sched-pool (java.util.concurrent.ScheduledThreadPoolExecutor. 2)]
      (when-not (:no-swank opts)
        (swank.swank/start-server :port (:swank-port opts)))
      (println "reloading data...")
      (scrape/reload-data (:data-dir opts))
      (println "starting issue cutter...")
      (scrape/issue-cutter (:data-dir opts) sched-pool (:now opts))
      (println "starting work-set updater...")
      (scrape/work-set-updater (:data-dir opts) sched-pool (:wait opts))
      (println "starting jetty...")
      (run-jetty #'app {:port (:jetty-port opts)}))))
