(ns klaxon.logging
  ;; This namespace defines logging protocols and implementations.
  (:require [clojure.spec.alpha :as s]))

(defprotocol Logger
  ;; For program functionality
  (debug!  [self msg])
  (warn!   [self msg])
  (error!  [self msg])
  (fatal!  [self msg])

  ;; For business logic
  (info!   [self msg])
  (notify! [self msg])
  (alert!  [self msg]))

(def logger? (partial satisfies? Logger))

(s/def ::logger logger?)

(s/def ::logger (s/keys :req [::debug! ::warn! ::error! ::fatal! ::info! ::notify! ::alert!]))

(defn dummy-logger []
  ;; A no-op logger implementation for testing or disabling logging.
  (reify Logger
    (debug!  [_ _] nil)
    (warn!   [_ _] nil)
    (error!  [_ _] nil)
    (fatal!  [_ _] nil)

    (info!   [_ _] nil)
    (notify! [_ _] nil)))

(defn print-logger []
  (reify Logger
    (debug!  [_ msg] (println msg))
    (warn!   [_ msg] (println msg))
    (error!  [_ msg] (println msg))
    (fatal!  [_ msg] (println msg))

    (info!   [_ msg] (println msg))
    (notify! [_ msg] (println msg))))
