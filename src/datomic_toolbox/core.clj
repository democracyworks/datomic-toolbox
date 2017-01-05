(ns datomic-toolbox.core
  (:require [clojure.tools.logging :as log]
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
(def default-migration-tx-instant (atom nil))
(def default-create-database-attempts (atom 5))

(defn configure! [{:keys [uri partition migration-tx-instant
                          create-database-attempts]}]
  (reset! default-uri uri)
  (reset! default-partition partition)
  (reset! default-connection nil)
  (reset! default-migration-tx-instant migration-tx-instant)
  (reset! default-create-database-attempts
          (or create-database-attempts 5)))

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

(defn migration-tx-instant [] @default-migration-tx-instant)

(defn create-database-attempts [] @default-create-database-attempts)

(defn transact [tx-data]
  (d/transact (connection) tx-data))

(defn applied-migrations []
  (migration/applied (db)))

(defn unapplied-migrations [directory]
  (migration/unapplied (db) directory))

(defn install-migration-schema []
  (migration/install-schema (connection) (partition) (migration-tx-instant)))

(defn run-migration [file]
  (migration/run (connection) file (migration-tx-instant)))

(defn run-migrations
  ([] (migration/run-all (connection) (db) nil (migration-tx-instant)))
  ([directory] (migration/run-all (connection) (db) directory (migration-tx-instant))))

(defn create-database-with-retries
  "Runs datomic.api/create-database on the `uri` arg but catches any exceptions
  it throws and tries again up to `tries` (or the configured
  default-database-attempts) with increasingly long sleeps in between. Returns
  the datomic.api/create-database return value if that ever succeeds, or
  re-throws the final exception if it does not."
  ([uri] (create-database-with-retries uri (create-database-attempts)))
  ([uri tries]
   (loop [attempt 1]
     (let [create-result (try
                           (d/create-database uri)
                           (catch Exception e
                             (log/warn "Unable to create database:" uri
                                       "-" (.getMessage e)
                                       "attempt:" attempt)
                             (when (>= attempt tries)
                               (throw e))))]
       (if (nil? create-result)
         (do (Thread/sleep (* attempt 1000))
             (recur (inc attempt)))
         (do (log/info "Created database:" uri)
             create-result))))))

(defn initialize [& [config]]
  (when config (configure! config))
  (create-database-with-retries (uri))
  (install-migration-schema)
  (run-migrations "datomic-toolbox-schemas")
  (run-migrations))
