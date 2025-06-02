(ns klaxon.order
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]

            [klaxon.common :as common :refer [*-> convert-keys]]

            [camel-snake-kebab.core :refer [->kebab-case-keyword]])
  (:import [java.time Duration Instant]))

(defn- fn-order-to [type]
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

(defn order? [o] (s/valid? ::order o))

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
