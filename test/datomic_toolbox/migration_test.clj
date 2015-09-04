(ns datomic-toolbox.migration-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox.migration :refer :all]
            [datomic-toolbox.schema-resource :as schema]
            [datomic.api :as d]
            [datomic-toolbox.core :as core]))

(defn migration-ready-db [f]
  (when-let [old-uri (core/uri)]
    (d/delete-database old-uri))
  (let [new-uri (str "datomic:mem://datomic-toolbox-test-" (java.util.UUID/randomUUID))]
    (d/create-database new-uri)
    (core/configure! {:uri new-uri :partition "datomic-toolbox-test-partition"}))
  (core/install-migration-schema)
  (f))

(use-fixtures :each migration-ready-db)

(deftest applied-test
  (testing "no applied migratons"
    (is (empty? (applied (core/db)))))
  (testing "with an applied migration"
    (run (core/connection) (first (schema/files)))
    (is ((applied (core/db)) "001-schema.edn")))
  (testing "with all migrations applied"
    (run-all (core/connection) (core/db))
    (is (= 3 (count (applied (core/db)))))))

(deftest unapplied-migrations-test
  (testing "no applied migrations"
    (is (= 3 (count (unapplied (core/db))))))
  (testing "with an applied migration"
    (run (core/connection) (first (schema/files)))
    (is (= 2 (count (unapplied (core/db)))))
    (is (= "002-schema.edn" (-> (unapplied (core/db)) first (.getName)))))
  (testing "with all migrations applied"
    (run-all (core/connection) (core/db))
    (is (empty? (unapplied (core/db))))))
