(ns datomic-toolbox.schema-resource-test
  (:require [datomic-toolbox.schema-resource :refer :all]
            [clojure.test :refer :all]))

(deftest files-test
  (testing "finds schema files"
    (is (= 3 (count (files))))
    (let [filenames (->> (files)
                         (map #(.getName %))
                         set)]
      (is (filenames "001-schema.edn"))
      (is (filenames "002-schema.edn"))
      (is (filenames "003-unique-id.edn"))
      (is (= "001-schema.edn" (-> (files) first (.getName)))))))
