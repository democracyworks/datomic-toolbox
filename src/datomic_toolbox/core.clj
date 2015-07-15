(ns datomic-toolbox.core
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [datomic.api :as d]
            [turbovote.resource-config :refer [config]])
  (:refer-clojure :exclude [partition]))

(defn connection []
  (d/connect (config :datomic :uri)))

(def db (comp d/db connection))

(defn transact [tx-data]
  (d/transact (connection) tx-data))

(defn partition [] (config :datomic :partition))

(defprotocol INamedResource
  (resource-name [resource]))
(extend-type java.net.URL
  INamedResource
  (resource-name [url]
    (-> url
        str
        (clojure.string/split #"/(?=.)")
        last)))
(extend-type java.io.File
  INamedResource
  (resource-name [file]
    (.getName file)))

(defn jarred-schemas [resource]
  (->> resource
       .getPath
       (re-find #"^[^:]*:(.*)!")
       second
       java.util.jar.JarFile.
       .entries
       enumeration-seq
       (filter #(.startsWith (str %) "schemas/"))
       (map (comp io/resource str))))

(defn vfs-schemas [resource]
  (->> resource
       .getContent
       .getChildren
       (map #(.getPhysicalFile %))))

(defn schema-files []
  (let [resource (io/resource "schemas")
        files    (condp = (.getProtocol resource)
                   "jar" (jarred-schemas resource)
                   "vfs" (vfs-schemas resource)
                   (-> resource io/as-file file-seq))]
    (->> files
         (filter #(.endsWith (resource-name %) ".edn"))
         (sort-by #(resource-name %)))))

(defn applied-migrations []
  (->> (d/q '[:find ?migration
              :in $
              :where
              [?e :datomic-toolbox/migration ?migration]] (db))
       (map first)
       set))

(defn unapplied-migrations []
  (let [applied? (fn [file]
                   ((applied-migrations) (resource-name file)))]
    (remove applied? (schema-files))))

(defn file->tx-data [file]
  (->> file slurp (clojure.edn/read-string {:readers *data-readers*})))

(defn run-migration [file]
  (let [migration-tx (file->tx-data file)
        full-tx (conj migration-tx {:db/id #db/id[:db.part/tx]
                                    :datomic-toolbox/migration (resource-name file)})]
    (-> full-tx transact deref)))

(defn run-migrations []
  (doseq [file (unapplied-migrations)]
    (run-migration file)))

(defn install-migration-schema []
  (-> [{:db/id #db/id[:db.part/db]
        :db/ident (partition)
        :db.install/_partition :db.part/db}
       {:db/id #db/id[:db.part/db]
        :db/ident :datomic-toolbox/migration
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/doc "Migration File Name"
        :db.install/_attribute :db.part/db}]
      transact
      deref))

(defn initialize []
  (d/create-database (config :datomic :uri))
  (install-migration-schema)
  (run-migrations))

(defn tempid
  ([]  (d/tempid (partition)))
  ([n] (d/tempid (partition) n)))

(defn dissoc-optional-nil-values [tx-map nullable-keys]
  (let [nulled-keys (filter (comp nil? tx-map) nullable-keys)]
    (apply dissoc tx-map nulled-keys)))

(defn- tx-data-fn-docstring [params]
  (apply str
         (interpose
          "\n  "
          ["Returns Datomic transaction data from a map of attributes, or"
           "from a Datomic database id and a map of attributes"
           ""
           "The attribute map can contain the following keys:"
           (apply str (interpose " " (map keyword params)))])))

(defmacro deftx-data-fn
  "Creates a fn with the given name with two arities, the one given in
   params, and a version with a datomic entity id prepended. The
   entity id can be referenced in the body as ?id.

   For example:

     (deftx-data-fn name-person-assoc-pet-tx
       [first last pet-id]
       [{:db/id ?id
         :person/first first
         :person/last last}
        [:db/add pet-id :pet/owner ?id]])

     (name-person-assoc-pet-tx
       {:first \"Chris\" :last \"Shea\" :pet-id 27081977})

     ;; or, if you have a specific id for the person
     ;; because you're using it in other transactions
     ;; you're building:
     (name-person-assoc-pet-tx
       27081976 {:first \"Chris\" :last \"Shea\" :pet-id 27081977})

   Either way, you get back tx-data that is internally consistent with
   the ?id for the person."
  [name params & body]
  (let [id-sym (gensym 'id)
        body-with-id (walk/prewalk-replace {'?id id-sym} body)
        docstring (tx-data-fn-docstring params)]
    `(defn ~name
       ~docstring
       ([data-map#] (~name (tempid) data-map#))
       ([~id-sym data-map#]
          (let [{:keys ~params} data-map#]
            ~@body-with-id)))))

(defn find-one
  "Given a query that returns only entity ids, a db value and query
  arguments, return the db entity that the query returns"
  [query db & args]
  (let [results (apply d/q query db args)]
    (->> results
         ffirst
         (d/entity db))))

(defn match-query
  "Given a db and a map of fields to values or a seq of two-element
  vectors of fields and values, performs a query for exact matches
  against all non-nil fields."
  [db fields]
  (let [non-nils (filter (comp not nil? second) fields)
        ->sym (fn [key] (->> key name (str \?) symbol))
        clause (fn [[key val]]
                 (vector '?e
                         key
                         (->sym key)))]
    (if (empty? non-nils)
      #{}
      (apply d/q (concat '[:find ?e :in $]
                         (map (comp ->sym first) non-nils)
                         '[:where]
                         (map clause non-nils))
             db
             (map second non-nils)))))

(defn match-entities
  "Given a db and a map of fields to values, performs a query for
   exact matches against all non nil fields, returning datomic
   entities"
  [db fields]
  (map (comp (partial d/entity db) first) (match-query db fields)))

(defn timestamps
  "Returns a hash with keys :created-at, :updated-at, and :timestamps,
   the latter being all the timestamps of transactions affecting the
   entity in ascending order.
   Pass in the entity or a database and the entity id."
  ([entity]
     (timestamps (d/entity-db entity) (:db/id entity)))
  ([db id]
      (let [tx-instants (->> (d/q '[:find ?txInstant
                                    :in $ ?e
                                    :where
                                    [?e _ _ ?tx]
                                    [?tx :db/txInstant ?txInstant]]
                                  (d/history db) id)
                             (map first)
                             sort)]
        {:created-at (first tx-instants)
         :updated-at (last tx-instants)
         :timestamps tx-instants})))
