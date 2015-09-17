(ns datomic-toolbox.test-helpers
  (:require [datomic-toolbox.core :as core]
            [datomic.api :as d]))

(def test-db-uri-root "datomic:mem://datomic-toolbox-test")

(defn new-test-db-uri []
  (when-let [old-uri (core/uri)]
    (d/delete-database old-uri))
  (str test-db-uri-root "-" (java.util.UUID/randomUUID)))

(defn unmigrated-test-db [test]
  (let [new-uri (new-test-db-uri)]
    (d/create-database new-uri)
    (core/configure! {:uri new-uri :partition "datomic-toolbox-test-partition"}))
  (core/install-migration-schema)
  (test))

(defn migrated-test-db [test]
  (let [new-uri (new-test-db-uri)]
    (core/initialize {:uri new-uri :partition "datomic-toolbox-test-partition"}))
  (test))
