(defproject democracyworks/datomic-toolbox "2.0.4-SNAPSHOT"
  :description "Datomic utilities"
  :url "http://github.com/democracyworks/datomic-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-pro "0.9.5394"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.6"]]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env]
                                   :password [:gpg :env]}}
  :profiles {:dev {:resource-paths ["dev-resources"]}
             :test {:resource-paths ["test-resources"]}}
  :deploy-repositories {"releases" :clojars})
