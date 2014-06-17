(defproject turbovote.datomic-toolbox "0.2.2-SNAPSHOT"
  :description "Datomic utilities"
  :url "http://github.com/turbovote/datomic-toolbox"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-pro "0.9.4755"]
                 [turbovote.resource-config "0.1.0"]]
  :profiles {:dev {:resource-paths ["dev-resources"]}
             :test {:resource-paths ["test-resources"]}})
