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
  (GET "/latest" [] (response (prn-str (scrape/latest-stories))))
  (GET "/issue" [d] (response (-> d Long. java.util.Date.
                                  scrape/get-stories prn-str)))
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
                  (optional ["-ns" "--no-swank" :default false]))]
    (when-not (:no-swank opts)
      (swank.swank/start-server :port (:swank-port opts)))
    (run-jetty #'app {:port (:jetty-port opts)})))
