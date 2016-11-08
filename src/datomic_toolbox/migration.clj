(ns datomic-toolbox.migration
  (:require [datomic.api :as d]
            [datomic-toolbox.schema-resource :as schema]))

(defn applied [db]
  (->> (d/q '[:find ?migration
              :where [?e :datomic-toolbox/migration ?migration]]
            db)
       (map first)
       set))

(defn applied? [db file]
  ((applied db) (schema/resource-name file)))

(defn unapplied
  [db directory]
  (remove (partial applied? db) (schema/files directory)))

(defn run
  ([connection file]
   (run connection file nil))
  ([connection file tx-instant]
   (let [migration-tx (schema/file->tx-data file)
         tx {:db/id #db/id[:db.part/tx]
             :datomic-toolbox/migration (schema/resource-name file)}
         tx-with-instant (if tx-instant
                           (assoc tx :db/txInstant tx-instant)
                           tx)
         full-tx (conj migration-tx tx-with-instant)]
     (->> full-tx (d/transact connection) deref))))

(defn run-all
  ([connection db] (run-all connection db nil))
  ([connection db directory] (run-all connection db directory nil))
  ([connection db directory tx-instant]
   (let [dir (or directory "schemas")]
     (doseq [file (unapplied db dir)]
       (run connection file tx-instant)))))

(defn set-tx-instant [tx-instant tx-data]
  (if tx-instant
    (conj tx-data
          {:db/id #db/id [:db.part/tx]
           :db/txInstant tx-instant})
    tx-data))

(defn install-schema
  ([connection partition]
   (install-schema connection partition nil))
  ([connection partition tx-instant]
   (->> [{:db/id #db/id[:db.part/db]
          :db/ident partition
          :db.install/_partition :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :datomic-toolbox/migration
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db/doc "Migration File Name"
          :db.install/_attribute :db.part/db}]
        (set-tx-instant tx-instant)
        (d/transact connection)
        deref)))
