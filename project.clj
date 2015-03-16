(defproject democracyworks.datomic-toolbox "0.2.5"
  :description "Datomic utilities"
  :url "http://github.com/democracyworks/datomic-toolbox"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-pro "0.9.5130"]
                 [turbovote.resource-config "0.1.4"]]
  :profiles {:dev {:resource-paths ["dev-resources"]}
             :test {:resource-paths ["test-resources"]}})
