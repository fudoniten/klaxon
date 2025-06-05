(ns klaxon.order
  ;; This namespace defines the structure and behavior of orders.
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]

            [klaxon.common :as common :refer [*-> convert-keys]]

            [camel-snake-kebab.core :refer [->kebab-case-keyword]])
  (:import [java.time Duration Instant]))

(s/def ::order (s/keys :req [::id ::filled-size ::filled-value ::fee ::completion-percentage ::product-id ::last-fill-time ::created-time ::average-filled-price ::settled? ::total-value ::total-fees ::product-type ::status ::side ::user-id ::order-type]))

(defn- fn-order-to [type]
  ;; Helper function to define a spec for order-related functions.
  (s/fspec :args (s/cat :order ::order)
           :ret  type))

(s/def sale? (fn-order-to boolean?))
(s/def buy? (fn-order-to boolean?))
(s/def age (fn-order-to ::common/duration))
(s/def time-since-fill (fn-order-to ::common/duration))

(def timestamp? (partial instance? Instant))

(s/def ::id uuid?)
(s/def ::filled-size decimal?)
(s/def ::filled-value decimal?)
(s/def ::fee decimal?)
(s/def ::completion-percentage (s/and decimal? #(<= 0 % 100)))
(s/def ::product-id decimal?)
(s/def ::last-fill-time timestamp?)
(s/def ::created-time timestamp?)
(s/def ::average-filled-price decimal?)
(s/def ::settled? boolean?)
(s/def ::total-value decimal?)
(s/def ::total-fees decimal?)
(s/def ::product-type #{:btc-usd :eth-usd :ada-usd :eth-btc :ada-btc})
(s/def ::status #{:pending
                  :open
                  :filled
                  :cancelled
                  :expired
                  :failed
                  :unknown-order-status
                  :queued
                  :cancel-queued})
(s/def ::side #{:buy :sell})
(s/def ::user-id uuid?)
(s/def ::order-type #{:unknown-trigger-status :invalid-order-type :stop-pending :stop-triggered})

(defn stop-triggered?
  ;; Checks if an order has been stop-triggered.
  [o]
  (-> o ::order-type (= :stop-triggered)))

(def order-size ::size)
(def filled-size ::filled-size)
(def product-type ::product-type)
(def id ::id)
(def order-type ::order-type)
(def created ::created)
(def status ::status)
(def total-value ::total-value)
(def average-filled-price ::average-filled-price)
(def total-fees ::total-fees)
(def side ::side)
(def completion-percentage ::completion-percentage)

(defn instantiate-order
  ;; Converts raw order data into a structured order map.
  [order]
  (let [keywordize (*-> str/lower-case keyword)
        bigdec-or-zero (fn [s] (if (= s "") 0 (bigdec s)))]
    (-> order
        (convert-keys :order_id                ::id                    common/to-uuid
                      :filled_size             ::filled-size           bigdec-or-zero
                      :filled_value            ::filled-value          bigdec-or-zero
                      :fee                     ::fee                   bigdec-or-zero
                      :completion_percentage   ::completion-percentage bigdec-or-zero
                      :product_id              ::product-id            keywordize
                      :last_fill_time          ::last-fill-time        common/parse-timestamp
                      :created_time            ::created-time          common/parse-timestamp
                      :average_filled_price    ::average-filled-price  bigdec-or-zero
                      :settled                 ::settled?              identity
                      :total_value_after_fees  ::total-value           bigdec-or-zero
                      :total_fees              ::total-fees            bigdec-or-zero
                      :product_type            ::product-type          keywordize
                      :status                  ::status                keywordize
                      :side                    ::side                  keywordize
                      :user_id                 ::user-id               common/to-uuid
                      :order_type              ::order-type            keywordize))))

(s/def ::order
  (s/keys
   :req [::id
         ::filled-size
         ::filled-value
         ::fee
         ::completion-percentage
         ::product-id
         ::last-fill-time
         ::created-time
         ::average-filled-price
         ::settled?
         ::total-value
         ::total-fees
         ::product-type
         ::status
         ::side
         ::user-id
         ::order-type]))

(defn order? [o]
  ;; Validates if the given object is a valid order.
  (s/valid? ::order o))

(defn sale? [o] (= (::side o) :sell))
(defn buy? [o] (= (::side o) :buy))
(defn age [o] (Duration/between (::created-time o) (Instant/now)))
(defn time-since-fill [o] (Duration/between (::last-fill-time o) (Instant/now)))

;; Anything other than USD is an error for now
(defn- product-id? [product]
  (and (string? product)
       (not (nil? (re-matches #"^[A-Z]{2,5}-USD$" product)))))

(defn- must-be [k v]
  (fn [o] (= (k o) v)))

(defn- ensure-relationship [pred k0 k1]
  (fn [o] (pred (k0 o) (k1 o))))

(s/def ::buy (s/and ::order buy?))
(s/def ::sale (s/and ::order sale?))

(s/def ::base-order
  (s/keys :req [::product-id
                ::type
                ::side
                ::price
                ::size]))

(s/def ::buy-order
  (s/and ::base-order (must-be ::side :buy)))

(s/def ::sell-order
  (s/and ::base-order (must-be ::side :sell)))

(s/fdef total-value
  :args (s/cat :order ::order)
  :ret  decimal?)
(def total-value ::total-value)

(s/fdef filled?
  :args (s/cat :order ::order)
  :ret  boolean?)
(defn filled? [o]
  (-> o ::status (= :filled)))

(s/fdef stop-triggered?
  :args (s/cat :order ::order)
  :ret  boolean?)
(defn stop-triggered? [o]
  (-> o ::order-type (= :stop-triggered)))
(ns klaxon.order-test
  (:require [clojure.test :refer :all]
            [klaxon.order :as order]))

(deftest test-instantiate-order
  (testing "Instantiate order with valid data"
    (let [order-data {:order_id "123e4567-e89b-12d3-a456-426614174000"
                      :filled_size "10.5"
                      :filled_value "105.0"
                      :fee "0.5"
                      :completion_percentage "100"
                      :product_id "BTC-USD"
                      :last_fill_time "2025-06-05T12:00:00Z"
                      :created_time "2025-06-05T11:00:00Z"
                      :average_filled_price "10.0"
                      :settled true
                      :total_value_after_fees "104.5"
                      :total_fees "0.5"
                      :product_type "btc-usd"
                      :status "filled"
                      :side "buy"
                      :user_id "123e4567-e89b-12d3-a456-426614174001"
                      :order_type "stop-triggered"}]
      (is (order/order? (order/instantiate-order order-data))))))

(deftest test-sale?
  (testing "Check if order is a sale"
    (let [order {:side :sell}]
      (is (order/sale? order)))))

(deftest test-buy?
  (testing "Check if order is a buy"
    (let [order {:side :buy}]
      (is (order/buy? order)))))
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
