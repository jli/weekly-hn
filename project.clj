(defproject weekly-hn "0.1-SNAPSHOT"
  :description "Hacker News at a more manageable update frequency"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring/ring-core "0.3.11"]
                 [ring/ring-devel "0.3.11"]
                 [ring/ring-jetty-adapter "0.3.11"]
                 [amalloy/ring-gzip-middleware "0.1.0"]
                 [compojure "0.6.5"]
                 [org.clojure/tools.cli "0.1.0"]
                 [enlive "1.0.0"]]
  :main weekly-hn.core)
