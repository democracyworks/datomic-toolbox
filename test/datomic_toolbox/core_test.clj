(ns datomic-toolbox.core-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox.core :refer :all]
            [turbovote.resource-config :refer [config]]
            [datomic.api :as d])
  (:refer-clojure :exclude [partition]))

(defn recreate-db
  [f]
  (d/delete-database (config :datomic :uri))
  (initialize)
  (f))

(use-fixtures :each recreate-db)

(deftest initialize-test
  ;; initialize is implicitly called via the recreate-db fixture
  (is (seq (d/q '[:find ?e :where [?e :db/ident :test/name]] (db)))))

(deftest tempid-test
  (let [id (tempid)]
    (is (= (:part id) (partition)))))

(deftest dissoc-optional-nil-values-test
  (is (= {}
         (dissoc-optional-nil-values {} [])))
  (is (= {:a 1 :b 2}
         (dissoc-optional-nil-values {:a 1 :b 2 :c nil} [:c])))
  (is (= {:a 1 :b nil}
         (dissoc-optional-nil-values {:a 1 :b nil :c nil :d nil} [:c :d])))
  (is (= {:a 1} (dissoc-optional-nil-values {:a 1} [:a]))))

(deftest deftx-data-fn-test
  (deftx-data-fn test-tx-data-fn [name important]
    (let [new-name (str "test-" name)]
      [{:db/id ?id
        :test/name new-name
        :test/important? important}]))
  (testing "without an explicit id"
    (let [tx-data (test-tx-data-fn {:name "the-macro" :important true})
          entity-map (first tx-data)]
      (is (= "test-the-macro") (:test/name entity-map))
      (is (:test/important? entity-map))
      (is (= (partition) (-> entity-map :db/id :part)))))
  (testing "with an explicit id"
    (let [id (tempid)
          tx-data (test-tx-data-fn id {:name "explicit-id" :important true})
          entity-map (first tx-data)]
      (is (= "test-explicit-id" (:test/name entity-map)))
      (is (= id (:db/id entity-map))))))

(deftest query-helper-tests
  (let [db (:db-after (d/with (db) [{:db/id (tempid)
                                     :test/name "test-name"
                                     :test/important? true}
                                    {:db/id (tempid)
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
    (let [test-id (tempid)
          {:keys [db-after tx-data tempids]} (d/with (db)
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
    (let [test-id (tempid)
          {:keys [db-after tx-data tempids]} (d/with (db)
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
