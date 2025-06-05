(ns klaxon.config
  ;; This namespace centralizes configuration settings for the Klaxon application.
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-config
  "Loads configuration from an EDN file."
  [filename]
  (with-open [r (io/reader filename)]
    (edn/read r)))

(def default-config
  {:ntfy-server "ntfy.sh"
   :poll-seconds 60})

(def config (atom default-config))

(defn get-config
  "Retrieves a configuration value by key."
  [key]
  (get @config key))
