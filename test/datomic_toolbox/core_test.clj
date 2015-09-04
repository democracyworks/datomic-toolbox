(ns datomic-toolbox.core-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox.core :refer :all]
            [datomic.api :as d])
  (:refer-clojure :exclude [partition]))

(defn recreate-db
  [f]
  (when-let [old-uri (uri)]
    (d/delete-database old-uri))
  (initialize {:uri (str "datomic:mem://datomic-toolbox-test-" (java.util.UUID/randomUUID))
               :partition :datomic-toolbox-test-partition})
  (f))

(use-fixtures :each recreate-db)

(deftest initialize-test
  ;; initialize is implicitly called via the recreate-db fixture
  (is (seq (d/q '[:find ?e :where [?e :db/ident :test/name]] (db)))))

(deftest tempid-test
  (let [id (tempid)]
    (is (= (:part id) (partition)))))
