(ns klaxon.order-flow
  (:require [clojure.core.async :refer [>! chan]]
            [clojure.core.async.flow :as flow]

            [clojure.spec.alpha :as s]

            [klaxon.client :as client]
            [klaxon.coinbase-api :as api]
            [klaxon.common :as common])

  (:import java.time.Instant))

;; (s/fdef stream-orders
;;   :args (s/cat :client ::client/client))
;; (defn stream-orders [client out-chan & {:keys [poll-delay start-date end-date]}]
;;   (loop []
;;     (let [orders (api/get-orders! client)]
;;       (if (api/error? orders)
;;         (>! out-chan {:type ::api/error :error orders})
;;         (for [order orders]
;;           (>! out-chan {:type ::api/success :value order})))
;;       (Thread/sleep poll-delay)
;;       (recur))))

(defn heartbeat [delay paused heartbeat-chan]
  (loop []
    (Thread/sleep @delay)
    (when-not @paused
      (>! heartbeat-chan {:timestamp (Instant/now)}))
    (recur)))

(defn heartbeat-flow
  ([] {:params {:heartbeat-delay "Time in seconds between heartbeats."}
       :outs   {:heartbeat "Channel on which heartbeat will be sent."}})

  ([{delay :heartbeat-time :as state}] ;; TODO: verify parameters
   (let [pause-atom (atom true)
         hb-chan (chan 10)
         hb-atom (atom delay)]
     (future (heartbeat hb-atom pause-atom hb-chan))
     (assoc state
            ::flow/in-ports {:heartbeat-in hb-chan}
            :paused         pause-atom
            :heartbeat      hb-atom)))

  ([state transition]
   (case transition
     ::flow/resume
     (do (reset! (:paused state) true)
         state)

     (::flow/pause ::flow/stop)
     (do (reset! (:paused state) false)
         state)))

  ([state in msg]
   (case in

     :heartbeat-in
     (let [{timestamp :timestamp} msg]
       [state {:timestamp timestamp}])

     (throw (ex-info (format "Unknown input: %s" in) {})))))

(defn filter-flow
  [filter-pred?]
  (flow/lift*->step (fn [i] (if (filter-pred? i) [i] []))))

;; (defn order-flow
;;   ([] {:params {:client "Coinbase API client"
;;                 :start-date "Timestamp from which to listen"}
;;        :outs   {:orders "Stream of recent orders"}})

;;   ([args] (assoc args :last-update (common/current-timestamp)))

;;   ([state _] state)

;;   ([{:keys [key hostname]} _in msg]))
