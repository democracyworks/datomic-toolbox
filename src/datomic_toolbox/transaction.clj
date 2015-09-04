(ns datomic-toolbox.transaction
  (:require [datomic.api :as d]
            [clojure.walk :as walk]
            [datomic-toolbox.core :as core]))

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
       ([data-map#] (~name (core/tempid) data-map#))
       ([~id-sym data-map#]
          (let [{:keys ~params} data-map#]
            ~@body-with-id)))))
