(ns datomic-toolbox.core
  (:require [clojure.java.io :as io]
            [datomic.api :as d]
            [datomic-toolbox.migration :as migration])
  (:refer-clojure :exclude [partition]))

(def default-uri (atom nil))
(def default-partition (atom nil))
(def default-connection
  "In theory this shouldn't be needed because datomic.api/connect is supposed to
  cache connections. But we saw 5+ second pauses sometimes when calling this, so
  I'm trying out storing it an atom instead of re-calling datomic.api/connect.
  - WSM 2016-2-13"
  (atom nil))

(defn configure! [{:keys [uri partition]}]
  (reset! default-uri uri)
  (reset! default-partition partition)
  (reset! default-connection nil))

(defn uri [] @default-uri)

(defn connection []
  (when (nil? @default-connection)
    (reset! default-connection (d/connect (uri))))
  @default-connection)

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
