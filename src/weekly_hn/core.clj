(ns weekly-hn.core
  (:use [compojure.core]
        [compojure.route :only [resources]]
        [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [response file-response]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.stacktrace :only [wrap-stacktrace]]
        [ring.middleware.gzip :only [wrap-gzip]]
        [clojure.tools.cli :only [cli optional]])
  (:require [swank.swank]
            [weekly-hn.scrape :as scrape])
  (:gen-class))

(defroutes base
  (GET "/love" [] (response "<3"))
  (GET "/index" [] (response (prn-str (scrape/archive-index))))
  (GET "/wip" [] (response (prn-str (scrape/issue-in-progress))))
  (GET "/issue" [d] (response (-> d Long. scrape/issue->stories prn-str)))
  (GET "/issue-take" [d n]
       (response (prn-str (scrape/take-issue-stories (Long. d) (Integer. n)))))
  (GET "/issue-drop" [d n]
       (response (prn-str (scrape/drop-issue-stories (Long. d) (Integer. n)))))
  (GET "/" [] (file-response "resources/public/index.html"))
  (resources "/")
  (ANY "*" [] (file-response "resources/public/index.html")))

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
                             :default 30.] #(Float. %)))]
    (let [sched-pool (java.util.concurrent.ScheduledThreadPoolExecutor. 2)]
      (when-not (:no-swank opts)
        (swank.swank/start-server :port (:swank-port opts)))
      (println "reloading data...")
      (scrape/reload-data (:data-dir opts))
      (println "starting issue cutter...")
      (scrape/issue-cutter (:data-dir opts) sched-pool)
      (println "starting work-set updater...")
      (scrape/work-set-updater (:data-dir opts) sched-pool (:wait opts))
      (println "starting jetty...")
      (run-jetty #'app {:port (:jetty-port opts)}))))
