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

;; one value

(deftest one-value-test
  (testing "setting one-value anew"
    (let [tempid (db/tempid)
          uuid (java.util.UUID/randomUUID)

          {:keys [db-after tempids]}
          @(d/transact (db/connection)
                       [[:transact tempid
                         :transaction-test/one-value nil uuid]])

          eid (d/resolve-tempid db-after tempids tempid)]
      (assert eid)
      (let [response (d/q '{:find [?uuid]
                            :in [$ ?eid]
                            :where [[?eid :transaction-test/one-value ?uuid]]}
                          db-after
                          eid)]
        (is (= uuid (first (first response)))))))
  (testing "setting existing one-value"
    (let [tempid (db/tempid)
          uuid (java.util.UUID/randomUUID)

          {:keys [db-after tempids]}
          @(d/transact (db/connection)
                       [[:db/add tempid
                         :transaction-test/one-value uuid]])

          eid (d/resolve-tempid db-after tempids tempid)
          uuid2 (java.util.UUID/randomUUID)

          {:keys [db-after]}
          @(d/transact (db/connection)
                       [[:transact eid :transaction-test/one-value uuid uuid2]])

          response (d/q '{:find [?uuid]
                          :in [$ ?eid]
                          :where [[?eid :transaction-test/one-value ?uuid]]}
                        db-after
                        eid)]
      (is (= uuid2 (first (first response))))))
  (testing "setting existing one-value with incorrect existing"
    (let [tempid (db/tempid)
          uuid (java.util.UUID/randomUUID)

          {:keys [db-after tempids]}
          @(d/transact (db/connection)
                       [[:db/add tempid
                         :transaction-test/one-value uuid]])

          wrong-uuid (java.util.UUID/randomUUID)
          eid (d/resolve-tempid db-after tempids tempid)
          uuid2 (java.util.UUID/randomUUID)]

      (is (thrown? Throwable
                   @(d/transact (db/connection)
                                [[:transact eid :transaction-test/one-value wrong-uuid uuid2]])))
      (let [response (d/q '{:find [?uuid]
                            :in [$ ?eid]
                            :where [[?eid :transaction-test/one-value ?uuid]]}
                          (db/db)
                          eid)]
        (is (= uuid (first (first response)))))))
  (testing "setting existing one-value to nil"
    (let [tempid (db/tempid)
          uuid (java.util.UUID/randomUUID)

          {:keys [db-after tempids]}
          @(d/transact (db/connection)
                       [[:db/add tempid
                         :transaction-test/one-value uuid]])

          eid (d/resolve-tempid db-after tempids tempid)

          {:keys [db-after]}
          @(d/transact (db/connection)
                       [[:transact eid :transaction-test/one-value uuid nil]])
          response (d/q '{:find [?uuid]
                          :in [$ ?eid]
                          :where [[?eid :transaction-test/one-value ?uuid]]}
                        (db/db)
                        eid)]
      (is (empty? response)))))

;; many value
;; setting anew
;; setting existing with correct existing value
;; setting existing with incorrect existing value

;; one ref
;; setting anew
;; setting existing with correct existing value
;; setting existing with incorrect existing value

;; many ref
;; setting anew
;; setting existing with correct existing value
;; setting existing with incorrect existing value
