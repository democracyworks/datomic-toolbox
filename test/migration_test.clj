(ns migration-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox :refer :all]
            [turbovote.resource-config :refer [config]]
            [datomic.api :as d])
  (:refer-clojure :exclude [partition]))

(defn migration-ready-db [f]
  (d/delete-database (config :datomic :uri))
  (d/create-database (config :datomic :uri))
  (install-migration-schema)
  (f))

(use-fixtures :each migration-ready-db)

(deftest schema-files-test
  (testing "finds schema files"
    (is (= 3 (count (schema-files))))
    (let [filenames (->> (schema-files)
                         (map #(.getName %))
                         set)]
      (is (filenames "001-schema.edn"))
      (is (filenames "002-schema.edn"))
      (is (filenames "003-unique-id.edn"))
      (is (= "001-schema.edn" (-> (schema-files) first (.getName)))))))

(deftest applied-migrations-test
  (testing "no applied migratons"
    (is (empty? (applied-migrations))))
  (testing "with an applied migration"
    (run-migration (first (schema-files)))
    (is ((applied-migrations) "001-schema.edn")))
  (testing "with all migrations applied"
    (run-migrations)
    (is (= 3 (count (applied-migrations))))))

(deftest unapplied-migrations-test
  (testing "no applied migrations"
    (is (= 3 (count (unapplied-migrations)))))
  (testing "with an applied migration"
    (run-migration (first (schema-files)))
    (is (= 2 (count (unapplied-migrations))))
    (is (= "002-schema.edn" (-> (unapplied-migrations) first (.getName)))))
  (testing "with all migrations applied"
    (run-migrations)
    (is (empty? (unapplied-migrations)))))
