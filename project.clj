(defproject democracyworks/datomic-toolbox "1.1.1-transact-SNAPSHOT"
  :description "Datomic utilities"
  :url "http://github.com/democracyworks/datomic-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-pro "0.9.5153"]]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env]
                                   :password [:gpg :env]}}
  :profiles {:dev {:resource-paths ["dev-resources"]}
             :test {:resource-paths ["test-resources"]}}
  :deploy-repositories {"releases" :clojars})
