(ns klaxon.core
  ;; Core namespace for monitoring and alerting on order statuses.
  (:require [clojure.string :as str]
            [clojure.core.async :refer [<! >! chan go-loop alt!]]
            [clojure.spec.alpha :as s]

            [klaxon.client :as client]
            [klaxon.order :as order]
            [klaxon.order-chan :refer [order-chan]]

            [pinger.core :as pinger])

  (:import [java.time Duration Instant]))

(def duration? (partial instance? Duration))

(s/fdef delayed-fill?
  :args (s/cat :threshold-age duration? :order ::order/order)
  :ret  boolean?)
(defn delayed-fill? [threshold-age order]
  "Checks if an order has been triggered but not filled within a threshold age."
  (and (> (order/age order) threshold-age)
       (not (order/filled? order))
       (order/stop-triggered? order)))

(defn positive-decimal? [o]
  (and (decimal? o) (>= o 0)))

(s/fdef over-threshold?
  :args (s/cat :threshold-value positive-decimal?
               :order           ::order/order)
  :ret  boolean?)
(defn over-threshold? [threshold-value order]
  "Determines if an order's total value exceeds a specified threshold."
  (> (order/total-value order) threshold-value))

(defn summarize-order
  "Generates a summary string for an order, detailing its key attributes."
  [o]
  (str/join "\n"
            [
             (format "%s order of %.2f %s @ %.2f"
                     (-> o order/side)
                     (-> o order/filled-size)
                     (-> o order/product-type name)
                     (-> o order/average-filled-price))
             "\n"
             (format "order id: %s" (order/id o))
             (format "order type: %s" (-> o order/order-type name))
             (format "created: %s" (order/created o))
             (format "status: %s" (-> o order/status name))
             (format "size: %.2f" (order/filled-size o))
             (format "product: %s" (-> o order/product-type name))
             (format "value: %.2f" (order/total-value o))
             (format "average price: %.2f" (order/average-filled-price o))
             (format "total fee: %.2f" (order/total-fees o))
             ]))

(defn summarize-large-fill
  ;; Summarizes a large order fill for alert notifications.
  [o]
  {:title (format "FILLED: %s $%.2f" (-> o order/side name) (order/total-value o))
   :body  (str "ORDER SUCCESSFULLY FILLED:\n\n"
               (summarize-order o))
   :type  :alert})

(defn summarize-small-fill
  [o]
  {:title (format "FILLED: %s $%.2f" (-> o order/side name) (order/total-value o))
   :body  (str "ORDER SUCCESSFULLY FILLED:\n\n"
               (summarize-order o))
   :type  :notify})

(defn summarize-delayed-fill
  [o]
  {:title (format "FAILED TO FILL: %s of %.2f failed after %s minutes! %.2f% filled."
                  (-> o (order/side) name)
                  (-> o (order/total-value))
                  (-> o (order/age) (.toMinutes))
                  (-> o (order/completion-percentage)))
   :body   (str "ORDER FAILED TO FILL:\n\n"
                (summarize-order o))
   :type   :alert})

(defn monitor-orders
  "Monitors orders for specific conditions and sends notifications based on thresholds."
  "Monitors orders for specific conditions and sends notifications based on thresholds."
  [orders
   & {:keys [threshold-value
             threshold-age
             notify
             stop
             err]
      :or   {threshold-value (bigdec 0)
             threshold-age   (Duration/ofMinutes 20)
             notify          (chan 10)
             stop            (chan)
             err             (chan 10)}}]
  (go-loop []
    (let [event (alt! (:out orders) ([order] {:type :order :order order})
                      (:err orders) ([err]   {:type :err :err err})
                      stop          ([_]     {:type :stop}))]
      (case (:type event)
        :stop  (do (println (format "stopping order monitor at %s" (Instant/now)))
                   (>! (:stop orders) :stop))
        :err   (let [{error :error} event]
                 (println (format "error from order stream: %s" error))
                 (>! err error)
                 (recur))
        :order (let [{order :order} event]
                 (if (over-threshold? threshold-value order)
                   (if (order/filled? order)
                     (>! notify (summarize-large-fill order))
                     (when (delayed-fill? threshold-age order)
                       (>! notify (summarize-delayed-fill order))))
                   (>! notify (summarize-small-fill order)))
                 (recur)))))
  {:out notify :stop stop :err err})

(defn monitor-and-alert
  "Handles notifications and alerts for order events, using the pinger module."
  [pinger notifications
   & {:keys [stop err]
      :or   {stop (chan)
             err  (chan 10)}}]
  (go-loop []
    (let [event (alt! (:out notifications) ([note]            {:type :notify :note note})
                      (:err notifications) ([{error :error}]  {:type :error  :error error})
                      stop                 ([_]               {:type :stop}))]
      (case (:type event)
        :stop   (do (println (format "stopping monitor-and-alert at %s" (Instant/now)))
                    (>! (:stop notifications) 1))
        :err    (let [{error :error} event]
                  (println (format "error from notify stream: %s" error))
                  (>! err error)
                  (recur))
        :notify (let [{{title :title body :body type :type} :note} event]
                  (case type
                    :notify (pinger/send!  pinger title body)
                    :alert  (pinger/alert! pinger title body))
                  (recur)))))
  {:stop stop :err err})
