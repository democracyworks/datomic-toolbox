(defproject democracyworks/datomic-toolbox "1.0.0"
  :description "Datomic utilities"
  :url "http://github.com/democracyworks/datomic-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-pro "0.9.5153"]
                 [turbovote.resource-config "0.1.4"]]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env]
                                   :password [:gpg :env]}}
  :profiles {:dev {:resource-paths ["dev-resources"]}
             :test {:resource-paths ["test-resources"]}})
