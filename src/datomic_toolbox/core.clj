(ns datomic-toolbox.core
  (:require [clojure.java.io :as io]
            [datomic.api :as d]
            [datomic-toolbox.migration :as migration])
  (:refer-clojure :exclude [partition]))

(def default-uri (atom nil))
(def default-partition (atom nil))

(defn configure! [{:keys [uri partition]}]
  (reset! default-uri uri)
  (reset! default-partition partition))

(defn uri [] @default-uri)

(defn connection []
  (d/connect (uri)))

(defn partition [] @default-partition)

(defn db [] (d/db (connection)))

(defn tempid
  ([]  (d/tempid (partition)))
  ([n] (d/tempid (partition) n)))

(defn transact [tx-data]
  (d/transact (connection) tx-data))

(defn applied-migrations []
  (migration/applied (db)))

(defn unapplied-migrations [directory]
  (migration/unapplied (db) directory))

(defn install-migration-schema []
  (migration/install-schema (connection) (partition)))

(defn run-migration [file]
  (migration/run (connection) file))

(defn run-migrations
  ([] (migration/run-all (connection) (db)))
  ([directory] (migration/run-all (connection) (db) directory)))

(defn initialize [& [config]]
  (when config (configure! config))
  (d/create-database (uri))
  (install-migration-schema)
  (run-migrations "datomic-toolbox-schemas")
  (run-migrations))
