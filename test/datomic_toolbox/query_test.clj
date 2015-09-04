(ns datomic-toolbox.query-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox.query :refer :all]
            [datomic.api :as d]
            [datomic-toolbox.core :as core]))

(deftest query-helper-tests
  (let [db (:db-after (d/with (core/db) [{:db/id (core/tempid)
                                          :test/name "test-name"
                                          :test/important? true}
                                         {:db/id (core/tempid)
                                          :test/name "test-2"
                                          :test/important? false}]))]
    (testing "find-one"
      (is (= "test-name" (-> '[:find ?e :where [?e :test/important? true]]
                             (find-one db)
                             :test/name))))
    (testing "match-query"
      (is (= "test-2" (->> (match-query db {:test/important? false})
                              ffirst
                              (d/entity db)
                              :test/name)))
      (is (= "test-2" (->> (match-query db [[:test/important? false]
                                            [:test/nil nil]])
                              ffirst
                              (d/entity db)
                              :test/name))))
    (testing "match-entities"
      (is (= "test-name" (-> (match-entities db {:test/important? true})
                             first
                             :test/name))))))

(deftest timestamps-test
  (testing "timestamps with db and id"
    (let [test-id (core/tempid)
          {:keys [db-after tx-data tempids]} (d/with (core/db)
                                                     [{:db/id test-id
                                                       :test/name "Alice"
                                                       :test/important? false}])
          test-id (d/resolve-tempid db-after tempids test-id)
          created-at (-> tx-data first (nth 2))
          _ (Thread/sleep 1)
          {:keys [db-after tx-data]} (d/with db-after
                                             [{:db/id test-id
                                               :test/important? true}])
          modified-at (-> tx-data first (nth 2))
          _ (Thread/sleep 1)
          {:keys [db-after tx-data]} (d/with db-after
                                             [{:db/id test-id
                                               :test/important? false}])
          updated-at (-> tx-data first (nth 2))
          timestamps (timestamps db-after test-id)]
      (is (= created-at (:created-at timestamps)))
      (is (= updated-at (:updated-at timestamps)))
      (is (= (list created-at modified-at updated-at)
             (:timestamps timestamps)))))
  (testing "timestamps with entity"
    (let [test-id (core/tempid)
          {:keys [db-after tx-data tempids]} (d/with (core/db)
                                                     [{:db/id test-id
                                                       :test/name "Alice"
                                                       :test/important? false}])
          test-id (d/resolve-tempid db-after tempids test-id)
          created-at (-> tx-data first (nth 2))
          _ (Thread/sleep 1)
          {:keys [db-after tx-data]} (d/with db-after
                                             [{:db/id test-id
                                               :test/important? true}])
          modified-at (-> tx-data first (nth 2))
          _ (Thread/sleep 1)
          {:keys [db-after tx-data]} (d/with db-after
                                             [{:db/id test-id
                                               :test/important? false}])
          updated-at (-> tx-data first (nth 2))
          entity (d/entity db-after test-id)
          timestamps (timestamps entity)]
      (is (= created-at (:created-at timestamps)))
      (is (= updated-at (:updated-at timestamps)))
      (is (= (list created-at modified-at updated-at)
             (:timestamps timestamps))))))
