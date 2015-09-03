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
    (is (= known-schema-files (map #(.getName %) (schema-files))))))

(deftest applied-migrations-test
  (let [migration-count (count (schema-files))]
   (testing "no applied migratons"
     (is (empty? (applied-migrations))))
   (testing "with an applied migration"
     (run-migration (first (schema-files)))
     (is ((applied-migrations) "001-schema.edn")))
   (testing "with all migrations applied"
     (run-migrations)
     (is (= migration-count (count (applied-migrations)))))))

(deftest unapplied-migrations-test
  (let [migration-count (count (schema-files))]
   (testing "no applied migrations"
     (is (= migration-count (count (unapplied-migrations)))))
   (testing "with an applied migration"
     (run-migration (first (schema-files)))
     (is (= (dec migration-count) (count (unapplied-migrations))))
     (is (= "002-schema.edn" (-> (unapplied-migrations) first (.getName)))))
   (testing "with all migrations applied"
     (run-migrations)
     (is (empty? (unapplied-migrations))))))
