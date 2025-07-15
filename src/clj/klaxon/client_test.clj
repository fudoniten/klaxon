(ns klaxon.client-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [klaxon.client :as client]
            [klaxon.jwt :as jwt]))

(deftest test-create-client
  (testing "Create client with valid hostname and key data"
    (let [hostname "api.coinbase.com"
          key-data {:key-name "test-key" :key "test-key-data"}]
      (is (client/create ::client/hostname hostname ::jwt/key-data key-data)))))

(run-tests)
