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

(deftest configure!-test
  (testing "resets connection atom to nil"
    ;; initialize is implicitly called via the recreate-db fixture
    (configure! {})
    (is (nil? @default-connection))))

(deftest initialize-test
  ;; initialize is implicitly called via the recreate-db fixture
  (is (seq (d/q '[:find ?e :where [?e :db/ident :test/name]] (db)))))

(deftest tempid-test
  (let [id (tempid)]
    (is (= (:part id) (partition)))))

(deftest create-database-with-retries-test
  (testing "retries after exceptions creating the database"
    (let [attempts (atom 0)]
      (with-redefs [d/create-database (fn [_]
                                        (if (< @attempts 3)
                                          (do (swap! attempts inc)
                                              (throw
                                                (Exception. "test exception")))
                                          true))]
        (is (create-database-with-retries "fake://uri" 4))))))