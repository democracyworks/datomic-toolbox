(ns datomic-toolbox.migration-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox.migration :refer :all]
            [datomic-toolbox.schema-resource :as schema]
            [datomic.api :as d]
            [datomic-toolbox.core :as core]
            [datomic-toolbox.test-helpers :refer [unmigrated-test-db]]))

(use-fixtures :each unmigrated-test-db)

(deftest applied-test
  (let [migration-count (count (schema/files "schemas"))]
    (testing "no applied migratons"
      (is (empty? (applied (core/db)))))
    (testing "with an applied migration"
      (run (core/connection) (first (schema/files "schemas")))
      (is (applied? (core/db) "001-schema.edn")))
    (testing "with all migrations applied"
      (run-all (core/connection) (core/db) "schemas")
      (is (= migration-count (count (applied (core/db))))))))

(deftest unapplied-test
  (let [migration-count (count (schema/files "schemas"))]
    (testing "no applied migrations"
      (is (= migration-count (count (unapplied (core/db) "schemas")))))
    (testing "with an applied migration"
      (run (core/connection) (first (schema/files "schemas")))
      (is (= (dec migration-count) (count (unapplied (core/db) "schemas"))))
      (is (= "002-schema.edn" (-> (unapplied (core/db) "schemas") first (.getName)))))
    (testing "with all migrations applied"
      (run-all (core/connection) (core/db) "schemas")
      (is (empty? (unapplied (core/db) "schemas"))))))
