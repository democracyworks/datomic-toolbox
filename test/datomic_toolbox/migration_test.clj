(ns datomic-toolbox.migration-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox.core :refer :all]
            [datomic.api :as d])
  (:refer-clojure :exclude [partition]))

(defn migration-ready-db [f]
  (when-let [old-uri (uri)]
    (d/delete-database old-uri))
  (let [new-uri (str "datomic:mem://datomic-toolbox-test-" (java.util.UUID/randomUUID))]
    (d/create-database new-uri)
    (configure! {:uri new-uri :partition "datomic-toolbox-test-partition"}))
  (install-migration-schema)
  (f))

(use-fixtures :each migration-ready-db)

(def known-schema-files
  ["001-schema.edn"
   "002-schema.edn"
   "003-unique-id.edn"
   "004-transaction-tests.edn"])

(deftest schema-files-test
  (testing "finds schema files"
    (is (= known-schema-files (map #(.getName %) (schema-files "schemas"))))))

(deftest applied-migrations-test
  (let [migration-count (count (schema-files "schemas"))]
   (testing "no applied migratons"
     (is (empty? (applied-migrations))))
   (testing "with an applied migration"
     (run-migration (first (schema-files "schemas")))
     (is ((applied-migrations) "001-schema.edn")))
   (testing "with all migrations applied"
     (run-migrations "schemas")
     (is (= migration-count (count (applied-migrations)))))))

(deftest unapplied-migrations-test
  (let [migration-count (count (schema-files "schemas"))]
   (testing "no applied migrations"
     (is (= migration-count (count (unapplied-migrations "schemas")))))
   (testing "with an applied migration"
     (run-migration (first (schema-files "schemas")))
     (is (= (dec migration-count) (count (unapplied-migrations "schemas"))))
     (is (= "002-schema.edn" (-> (unapplied-migrations "schemas") first (.getName)))))
   (testing "with all migrations applied"
     (run-migrations "schemas")
     (is (empty? (unapplied-migrations "schemas"))))))
