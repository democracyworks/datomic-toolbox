(ns datomic-toolbox.transaction-function-test
  (:require
   [datomic.api :as d]
   [datomic-toolbox.core :as db]
   [clojure.test :refer :all]))

(def test-db "datomic:mem://test-db")

(use-fixtures :each
  (fn [test]
    (d/delete-database test-db)
    (db/configure! {:uri test-db
                    :partition "test-db"})
    (db/initialize)
    (db/run-migrations)
    (test)))

(defn add-relation!
  "Given a relationships returns an eid, relationship, and uuid for
  the newly transacted entity."
  [rel constr]
  (let [tempid (db/tempid)
        value (constr)
        tx-data [{:db/id tempid
                  rel value}]
        {:keys [db-after tempids]} @(d/transact (db/connection) tx-data)
        eid (d/resolve-tempid db-after tempids tempid)]
    [eid rel value]))

(defn rand-uuid []
  (java.util.UUID/randomUUID))

(defn rand-uuids []
  (set [(rand-uuid) (rand-uuid) (rand-uuid)]))

;; one value

(deftest one-value-test
  (testing "setting one-value anew"
    (let [tempid (db/tempid)
          uuid (java.util.UUID/randomUUID)
          tx-data [[:transact tempid :transaction-test/one-value nil uuid]]
          {:keys [db-after tempids]} @(d/transact (db/connection) tx-data)
          eid (d/resolve-tempid db-after tempids tempid)
          entity (d/entity db-after eid)]
      (is (= uuid (:transaction-test/one-value entity)))))

  (testing "setting existing one-value"
    (let [[eid rel uuid] (add-relation! :transaction-test/one-value rand-uuid)
          uuid2 (java.util.UUID/randomUUID)
          tx-data [[:transact eid :transaction-test/one-value uuid uuid2]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (= uuid2 (:transaction-test/one-value entity)))))

  (testing "setting existing one-value over the same value"
    (let [[eid rel uuid] (add-relation! :transaction-test/one-value rand-uuid)
          tx-data [[:transact eid :transaction-test/one-value uuid uuid]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (= uuid (:transaction-test/one-value entity)))))

  (testing "setting existing one-value with incorrect existing"
    (let [[eid rel uuid] (add-relation! :transaction-test/one-value rand-uuid)
          wrong (java.util.UUID/randomUUID)
          uuid2 (java.util.UUID/randomUUID)
          tx-data [[:transact eid :transaction-test/one-value wrong uuid2]]]
      (is (thrown-with-msg? java.util.concurrent.ExecutionException
                            #"ConcurrentModificationException"
                            @(d/transact (db/connection) tx-data)))
      (let [entity (d/entity (db/db) eid)]
        (is (= uuid (:transaction-test/one-value entity))))))

  (testing "setting existing one-value to nil"
    (let [[eid rel uuid] (add-relation! :transaction-test/one-value rand-uuid)
          tx-data [[:transact eid :transaction-test/one-value uuid nil]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (nil? (:transaction-test/one-value entity)))))

  (testing "setting non-existant one-value to nil"
    (let [[eid rel uuid] (add-relation! :transaction-test/many-value rand-uuids)
          tx-data [[:transact eid :transaction-test/one-value nil nil]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (nil? (:transaction-test/one-value entity))))))

;; many value

(deftest many-value-test
  (testing "setting many-value anew"
    (let [tempid (db/tempid)
          uuids (set [(java.util.UUID/randomUUID) (java.util.UUID/randomUUID)])
          tx-data [[:transact tempid :transaction-test/many-value nil uuids]]
          {:keys [db-after tempids]} @(d/transact (db/connection) tx-data)
          eid (d/resolve-tempid db-after tempids tempid)
          entity (d/entity db-after eid)]
      (is (= uuids (:transaction-test/many-value entity)))))

  (testing "setting existing many-value"
    (let [[eid rel uuids] (add-relation! :transaction-test/many-value rand-uuids)
          uuids2 (rand-uuids)
          tx-data [[:transact eid :transaction-test/many-value uuids uuids2]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (= uuids2 (:transaction-test/many-value entity)))))

  (testing "setting existing many-value over the same value"
    (let [[eid rel uuids] (add-relation! :transaction-test/many-value rand-uuids)
          tx-data [[:transact eid :transaction-test/many-value uuids uuids]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (= uuids (:transaction-test/many-value entity)))))
  (testing "setting existing many-value with incorrect existing"
    (let [[eid rel uuids] (add-relation! :transaction-test/many-value rand-uuids)
          wrong (rand-uuids)
          uuids2 (rand-uuids)
          tx-data [[:transact eid :transaction-test/many-value wrong uuids2]]]
      (is (thrown-with-msg? java.util.concurrent.ExecutionException
                            #"ConcurrentModificationException"
                            @(d/transact (db/connection) tx-data)))
      (let [entity (d/entity (db/db) eid)]
        (is (= uuids (:transaction-test/many-value entity))))))
  (testing "setting existing many-value to nil"
    (let [[eid rel uuids] (add-relation! :transaction-test/many-value rand-uuids)
          tx-data [[:transact eid :transaction-test/many-value uuids nil]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (nil? (:transaction-test/many-value entity)))))
  (testing "setting non-existant many-value to nil"
    (let [[eid rel uuid] (add-relation! :transaction-test/one-value rand-uuid)
          tx-data [[:transact eid :transaction-test/many-value nil nil]]
          {:keys [db-after]} @(d/transact (db/connection) tx-data)
          entity (d/entity db-after eid)]
      (is (nil? (:transaction-test/many-value entity))))))

  ;; one ref
  ;; setting anew
  ;; setting existing with correct existing value
  ;; setting existing with incorrect existing value

  ;; many ref
  ;; setting anew
  ;; setting existing with correct existing value
  ;; setting existing with incorrect existing value
