(ns klaxon.order-chan
  (:require [clojure.core.async :refer [>! chan go-loop alt!]]
            [clojure.spec.alpha :as s]

            [klaxon.client :as client]
            [klaxon.coinbase-api :as api]
            [klaxon.common :as common]

            [fudo-clojure.result :as result])

  (:import java.time.Instant))

(defn order-chan
  [client &
   {:keys [delay start-time out err stop]
    :or   {delay      60
           start-time (Instant/now)
           out        (chan)
           err        (chan)
           stop       (chan)}}]
  (let [{heartbeat :heartbeat hb-stop :stop} (common/heartbeat-chan delay)]
    (go-loop [event (alt! heartbeat ([{timestamp :timestamp}] {:type :heartbeat :timestamp timestamp})
                          stop      ([_] {:type :stop}))
              start start-time]
      (if (= :heartbeat (:type event))
        (let [end    (-> event :timestamp)
              orders (api/get-orders! client {::api/start-date start ::api/end-date end})]
          (if (api/failure? orders)
            (>! err (ex-info (format "%s: Failed to connect to Coinbase API: %s"
                                     (Instant/now)
                                     (api/error-message orders))
                             {:error orders}))
            (doseq [order (result/unwrap orders)]
              (>! out order)))
          (recur (alt! heartbeat ([{timestamp :timestamp}] {:type :heartbeat :timestamp timestamp})
                       stop      ([_] {:type :stop}))
                 end))
        (do (println (format "stopping: %s" (:type event)))
            (>! hb-stop 1)))))
  {:out out :err err :stop stop})
