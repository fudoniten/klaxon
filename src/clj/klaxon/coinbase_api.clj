(ns klaxon.coinbase-api
  ;; This namespace interfaces with the Coinbase API for order data.
  (:require [clojure.string :as str]

            [klaxon.common :refer [->* convert-key]]
            [klaxon.client :as client]
            [klaxon.order :as order]
            [klaxon.common :as common]

            [fudo-clojure.http.request :as req]
            [fudo-clojure.result :as result :refer [map-success]]))

(def base-get-request
  (-> (req/base-request)
      (req/as-get)
      (req/with-header :content-type "application/json")
      (req/with-header :accept "application/json")))

(s/def ::api-query (s/keys :req-un [::start-date ::end-date ::product-id ::status]))

(defn filter-keys [m f]
  (into {} (filter (comp f val) m)))

(defn update-key
  [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn convert-key-if-preset [m ok nk f]
  (if (contains? m ok)
    (assoc m nk (f (get m ok)))
    m))

(defn prepare-query [query]
  (-> query
      (filter-keys some?)
      (convert-key-if-preset ::start-date :start_date str)
      (convert-key-if-preset ::end-date :end_date str)
      (convert-key-if-preset ::product-id :product_id (->* str str/upper-case))
      (convert-key-if-preset ::status :status (->* str str/upper-case))))

(defn error? [o] (result/failure? o))

(def error-message result/error-message)

(def failure? result/failure?)

(defn get-orders!
  ;; Fetches historical order data from the Coinbase API.
  ([client] (get-orders! client {}))
  ([client query]
   (let [result (client/get! client
                             (-> base-get-request
                                 (req/with-path (client/build-path :api
                                                                   :v3
                                                                   :brokerage
                                                                   :orders
                                                                   :historical
                                                                   :batch))
                                 (req/with_query_params (prepare-query query))))]
     (-> result
         (map-success :orders)
         (map-success (partial map order/instantiate-order))))))
