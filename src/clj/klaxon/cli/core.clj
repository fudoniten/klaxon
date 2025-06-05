(ns klaxon.cli.core
  ;; This namespace defines the command-line interface for the Klaxon application.
  (:require [clojure.core.async :refer [>!! <!! chan go-loop alt!]]
            [clojure.tools.cli :as cli]
            [clojure.set :as set]
            [clojure.string :as str]

            [klaxon.common :as common]
            [klaxon.client :as client]
            [klaxon.core :refer [monitor-and-alert]]
            [klaxon.config :as config]
            [klaxon.jwt :as jwt]
            [klaxon.order-chan :refer [order-chan]]
            [klaxon.logging :as logging]

            [pinger.core :as pinger])
  (:import java.time.Instant)
  (:gen-class))

(def cli-opts
  [["-v" "--verbose" "provide verbose output."]
   ["-h" "--help" "print this message."]

   ["-k" "--key-file KEYFILE" (str "JSON file containing JWT key data. Default: " (config/get-config :key-file))]
   ["-p" "--poll-seconds SECONDS" "Frequency with which to poll for activity."
    :default (config/get-config :poll-seconds)
    :parse-fn #(Integer/parseInt %)]

   ["-n" "--ntfy-server SERVER" "ntfy.sh server."
    :default (config/get-config :ntfy-server)]
   ["-t" "--ntfy-topic TOPIC" (str "ntfy.sh topic to which notifications will be sent. Default: " (config/get-config :ntfy-topic))]])

(s/def ::cli-options (s/keys :req-un [::key-file ::ntfy-topic ::poll-seconds]))

(defn- msg-quit
  [status msg]
  (logging/info! logger msg)
  (System/exit status))

(defn- usage
  ([summary] (usage summary []))
  ([summary errs] (str/join \newline
                            (concat errs
                                    ["usage: klaxon [opts]"
                                     ""
                                     "options:"
                                     summary]))))

(defn- parse-opts
  [args reqs cli-opts]
  (let [{:keys [options] :as result} (cli/parse-opts args cli-opts)
        missing (set/difference reqs (-> options (keys) (set)))
        missing-errs (map #(format "missing required parameter: %s" (name %))
                          missing)]
    (update result :errors concat missing-errs)))

(defn -main
  ;; Entry point for the Klaxon CLI application.
  [& args]
  (let [logger (logging/print-logger)
        required-args #{:key-file :ntfy-topic :poll-seconds}
        {:keys [options _ errors summary]} (parse-opts args required-args cli-opts)]
    (when (:help options) (msg-quit 0 (usage summary)))
    (when (seq errors) (msg-quit 1 (usage summary errors)))
    (let [{:keys [key-file
                  ntfy-server
                  ntfy-topic
                  poll-seconds
                  verbose]} options
          key-data (jwt/load-key-file key-file)]
      (when verbose (logging/info! logger (format "launching klaxon server")))
      (let [client        (client/create ::client/hostname "api.coinbase.com"
                                         ::jwt/key-data key-data)
            start         (Instant/now)
            shutdown-chan (chan)
            orders        (order-chan client :delay poll-seconds :start-time start)
            pinger        (pinger/open-channel ntfy-server ntfy-topic)
            {monitor-stop :stop errs :err} (monitor-and-alert pinger orders)]
        (.addShutdownHook (Runtime/getRuntime)
                          (Thread. (fn [] (>!! shutdown-chan true))))
        (go-loop []
          (let [evt (alt! errs          ([{error :error}] {:type :error :error error})
                          shutdown-chan ([_] {:type :shutdown}))]
            (case (:type evt)
              :error    (do (logging/error! logger (format "ERROR: %s" (:error evt)))
                            (recur))
              :shutdown (println "stopping error stream..."))))
        (<!! shutdown-chan)
        (println "stopping order monitor...")
        (>!! monitor-stop true)))
    (msg-quit :message "stopping klaxon server")))
