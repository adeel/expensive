(defproject expensive "1.0.0-SNAPSHOT"
  :description "A simple way to track expenses and how much money you currently have."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring/ring-core "1.1.0"]
                 [ring/ring-jetty-adapter "0.3.8"]
                 [compojure "1.0.4"]
                 [hiccup "1.0.0"]
                 [congomongo "0.1.7-SNAPSHOT"]
                 [clj-time "0.3.0"]
                 [sandbar/sandbar "0.4.0-SNAPSHOT"]
                 [org.clojars.adamwynne/mongodb-session "1.0.2"]
                 [clojure-csv/clojure-csv "2.0.0-alpha1"]]
  :dev-dependencies [[lein-ring "0.7.0"]
                     [org.slf4j/slf4j-simple "1.6.1"]]
  :ring {:handler expensive.core/app}
  :jvm-opts ["-Dfile.encoding=utf-8"])