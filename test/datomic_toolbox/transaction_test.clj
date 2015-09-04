(ns datomic-toolbox.transaction-test
  (:require [clojure.test :refer :all]
            [datomic-toolbox.transaction :refer :all]
            [datomic-toolbox.core :as core]))

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
      (is (= (core/partition) (-> entity-map :db/id :part)))))
  (testing "with an explicit id"
    (let [id (core/tempid)
          tx-data (test-tx-data-fn id {:name "explicit-id" :important true})
          entity-map (first tx-data)]
      (is (= "test-explicit-id" (:test/name entity-map)))
      (is (= id (:db/id entity-map))))))
