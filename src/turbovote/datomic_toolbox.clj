(ns turbovote.datomic-toolbox
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

(defn schema-files []
  (->> (io/resource "schemas")
       io/as-file
       file-seq
       (filter #(.endsWith (.getName %) ".edn"))))

(defn initialize []
  (d/create-database (config :datomic :uri))
  (doseq [file (schema-files)]
    (->> file
         slurp
         (clojure.edn/read-string {:readers *data-readers*})
         transact
         deref)))

(defn partition [] (config :datomic :partition))

(def tempid (comp d/tempid partition))

(defn dissoc-optional-nil-values [tx-map nullable-keys]
  (let [nulled-keys (filter (comp nil? tx-map) nullable-keys)]
    (apply dissoc tx-map nulled-keys)))

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
        body-with-id (walk/prewalk-replace {'?id id-sym} body)]
    `(defn ~name
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
  "Given a db and a map of fields to values, performs a query
   for exact matches against all non-nil fields."
  [db fields]
  (let [non-nils (into (sorted-map) (filter (comp not nil? second) fields))
        ->sym (fn [key] (->> key name (str \?) symbol))
        clause (fn [[key val]]
                 (vector '?e
                         key
                         (->sym key)))]
    (if (empty? non-nils)
      #{}
      (apply d/q (concat '[:find ?e :in $]
                         (map ->sym (keys non-nils))
                         '[:where]
                         (map clause non-nils))
             db
             (vals non-nils)))))

(defn match-entities
  "Given a db and a map of fields to values, performs a query for
   exact matches against all non nil fields, returning datomic
   entities"
  [db fields]
  (map (comp (partial d/entity db) first) (match-query db fields)))
