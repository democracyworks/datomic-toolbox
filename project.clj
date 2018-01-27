(defproject democracyworks/datomic-toolbox "2.0.5-SNAPSHOT"
  :description "Datomic utilities"
  :url "http://github.com/democracyworks/datomic-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.datomic/datomic-pro "0.9.5661"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.271"]
                 [org.clojure/tools.logging "0.4.0"]]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env]
                                   :password [:gpg :env]}}
  :profiles {:dev {:resource-paths ["dev-resources"]}
             :test {:resource-paths ["test-resources"]}}
  :deploy-repositories {"releases" :clojars})
