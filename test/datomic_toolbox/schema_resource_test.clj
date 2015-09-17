(ns datomic-toolbox.schema-resource-test
  (:require [datomic-toolbox.schema-resource :refer :all]
            [clojure.test :refer :all]))

(def known-schema-files
  ["001-schema.edn"
   "002-schema.edn"
   "003-unique-id.edn"
   "004-transaction-tests.edn"])

(deftest files-test
  (testing "finds schema files"
    (is (= known-schema-files (map #(.getName %) (files "schemas"))))))
