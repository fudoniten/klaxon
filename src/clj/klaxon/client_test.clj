(ns klaxon.client-test
  (:require [clojure.test :refer :all]
            [klaxon.client :as client]))

(deftest test-create-client
  (testing "Create client with valid hostname and key data"
    (let [hostname "api.coinbase.com"
          key-data {:key-name "test-key" :key "test-key-data"}]
      (is (client/create :hostname hostname :key-data key-data)))))
