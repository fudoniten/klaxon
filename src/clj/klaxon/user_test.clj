(ns klaxon.user-test
  (:require [clojure.test :refer :all]
            [klaxon.user :as user]))

(deftest test-user-id
  (testing "Check if user ID is a valid UUID"
    (let [valid-uuid "123e4567-e89b-12d3-a456-426614174000"]
      (is (user/id valid-uuid)))))
(ns klaxon.client-test
  (:require [clojure.test :refer :all]
            [klaxon.client :as client]))

(deftest test-create-client
  (testing "Create client with valid hostname and key data"
    (let [hostname "api.coinbase.com"
          key-data {:key-name "test-key" :key "test-key-data"}]
      (is (client/create :hostname hostname :key-data key-data)))))
(ns klaxon.cli.core-test
  (:require [clojure.test :refer :all]
            [klaxon.cli.core :as cli]))

(deftest test-parse-opts
  (testing "Parse CLI options with required arguments"
    (let [args ["-k" "keyfile.json" "-t" "topic" "-p" "60"]
          result (cli/parse-opts args #{:key-file :ntfy-topic :poll-seconds} cli/cli-opts)]
      (is (empty? (:errors result))))))
(ns klaxon.common-test
  (:require [clojure.test :refer :all]
            [klaxon.common :as common]))

(deftest test-round-to-dollar
  (testing "Round number to nearest dollar"
    (is (= 10M (common/round-to-dollar 10.49)))
    (is (= 11M (common/round-to-dollar 10.50)))))

(deftest test-round-to-cent
  (testing "Round number to nearest cent"
    (is (= 10.49M (common/round-to-cent 10.494)))
    (is (= 10.50M (common/round-to-cent 10.505)))))
(ns klaxon.core-test
  (:require [clojure.test :refer :all]
            [klaxon.core :as core]
            [klaxon.order :as order]))

(deftest test-delayed-fill?
  (testing "Check if order is delayed fill"
    (let [threshold-age (java.time.Duration/ofMinutes 20)
          order {:created-time (java.time.Instant/now)
                 :filled-size 0
                 :order-type :stop-triggered}]
      (is (core/delayed-fill? threshold-age order)))))
(ns klaxon.logging-test
  (:require [clojure.test :refer :all]
            [klaxon.logging :as logging]))

(deftest test-dummy-logger
  (testing "Dummy logger does not perform any actions"
    (let [logger (logging/dummy-logger)]
      (is (nil? (logging/debug! logger "test message"))))))
(ns klaxon.account-test
  (:require [clojure.test :refer :all]
            [klaxon.account :as account]))

(deftest test-account-balance
  (testing "Retrieve balance for a specific currency account"
    (let [accounts {:USD {:balance 1000}}
          currency :USD]
      (is (= 1000 (account/account-balance accounts currency))))))
(ns klaxon.jwt-test
  (:require [clojure.test :refer :all]
            [klaxon.jwt :as jwt]))

(deftest test-load-key
  (testing "Load key with valid key name and key data"
    (let [key-name "test-key"
          key "test-key-data"]
      (is (jwt/load-key key-name key)))))
(ns klaxon.coinbase-api-test
  (:require [clojure.test :refer :all]
            [klaxon.coinbase-api :as api]))

(deftest test-get-orders
  (testing "Fetch historical order data"
    (let [client (mock-client)
          result (api/get-orders! client)]
      (is (not (api/error? result))))))
(ns klaxon.order-chan-test
  (:require [clojure.test :refer :all]
            [klaxon.order-chan :as order-chan]))

(deftest test-order-chan
  (testing "Create order channel with default settings"
    (let [client (mock-client)
          chan (order-chan/order-chan client)]
      (is (map? chan)))))
(ns klaxon.types-test
  (:require [clojure.test :refer :all]
            [klaxon.types :as types]))

(deftest test-base64-string
  (testing "Check if string is valid base64"
    (let [valid-base64 "SGVsbG8gd29ybGQ="]
      (is (types/base64-string valid-base64)))))
(ns klaxon.product-test
  (:require [clojure.test :refer :all]
            [klaxon.product :as product]))

(deftest test-product-id
  (testing "Check if product ID is a valid UUID"
    (let [valid-uuid "123e4567-e89b-12d3-a456-426614174000"]
      (is (product/id valid-uuid)))))
