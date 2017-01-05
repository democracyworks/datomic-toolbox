(ns datomic-toolbox.query
  (:require [datomic.api :as d]))

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
