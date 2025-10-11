(ns klaxon.coinbase-api
  ;; This namespace interfaces with the Coinbase API for order data.
  (:require [clojure.string :as str]

            [klaxon.common :refer [->* convert-key]]
            [klaxon.client :as client]
            [klaxon.order :as order]

            [fudo-clojure.http.request :as req]
            [fudo-clojure.result :as result :refer [map-success failure]])

  (:import java.io.IOException
           java.io.InterruptedIOException            ; includes SocketTimeout on some JDKs
           java.net.SocketTimeoutException
           java.net.ConnectException
           java.net.NoRouteToHostException
           java.net.PortUnreachableException
           java.net.SocketException
           javax.net.ssl.SSLException
           java.nio.channels.ClosedChannelException
           java.net.http.HttpTimeoutException
           java.net.UnknownHostException))

(def base-get-request
  (-> (req/base-request)
      (req/as-get)
      (req/with-header :content-type "application/json")
      (req/with-header :accept "application/json")))

(defn filter-keys [m f]
  (into {} (filter (comp f val) m)))

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

(def error-message result/error-message)

(def failure? result/failure?)

(def ^:private transient-network-exceptions
  #{IOException
    InterruptedIOException
    SocketTimeoutException
    ConnectException
    NoRouteToHostException
    PortUnreachableException
    SocketException
    SSLException
    ClosedChannelException
    HttpTimeoutException
    UnknownHostException})

(def ^:private retryable-status?
  (let [codes #{408 421 425 429 500 502 503 504}]
    (fn [status] (contains? codes status))))

(defn any-instance?
  [^Throwable t classes]
  (some (fn [cls] (instance? cls t)) classes))

(defn transient-network-failure?
  [^Throwable e]
  (let [chain (take-while some? (iterate ex-cause e))]
    (boolean
     (or
      (some (fn [e] (any-instance? e transient-network-exceptions)) chain)
      (some (fn [e]
              (when (instance? clojure.lang.ExceptionInfo e)
                (when-let [st (:status (ex-data e))]
                  (retryable-status? st))))
            chain)))))

(defn get-orders!
  ;; Fetches historical order data from the Coinbase API.
  ([client] (get-orders! client {}))
  ([{:keys [::client/hostname] :as client} query]
   (try
     (let [result (client/get! client
                               (-> base-get-request
                                   (req/with-host hostname)
                                   (req/with-path (client/build-path :api
                                                                     :v3
                                                                     :brokerage
                                                                     :orders
                                                                     :historical
                                                                     :batch))
                                   (req/with_query_params (prepare-query query))))]
       (-> result
           (map-success :orders)
           (map-success (partial map order/instantiate-order))))
     (catch Throwable e
       (if (transient-network-failure? e)
         (result/failure (format "failed due to transient network error: %s" e) e)
         (throw e))))))
