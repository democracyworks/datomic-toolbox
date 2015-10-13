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
  [connection file]
  (let [migration-tx (schema/file->tx-data file)
        full-tx (conj migration-tx {:db/id #db/id[:db.part/tx]
                                    :datomic-toolbox/migration (schema/resource-name file)})]
    (->> full-tx (d/transact connection) deref)))

(defn run-all
  ([connection db] (run-all connection db "schemas"))
  ([connection db directory]
   (doseq [file (unapplied db directory)]
     (run connection file))))

(defn install-schema
  [connection partition]
  (->> [{:db/id #db/id[:db.part/db]
         :db/ident partition
         :db.install/_partition :db.part/db}
        {:db/id #db/id[:db.part/db]
         :db/ident :datomic-toolbox/migration
         :db/valueType :db.type/string
         :db/cardinality :db.cardinality/one
         :db/doc "Migration File Name"
         :db.install/_attribute :db.part/db}]
       (d/transact connection)
       deref))
