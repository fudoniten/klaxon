(ns klaxon.cli.core
  (:require [clojure.tools.cli :as cli]
            [clojure.set :as set]
            [clojure.string :as str])
  (:gen-class))

(def cli-opts
  [["-v" "--verbose" "provide verbose output."]
   ["-h" "--help" "print this message."]

   ])

(defn- msg-quit [status msg]
  (println msg)
  (System/exit status))

(defn- usage
  ([summary] (usage summary []))
  ([summary errs] (->> (concat errors
                               [["usage: klaxon [opts]"
                                 ""
                                 "options:"
                                 summary]])
                       (str/join \newline))))

(defn- parse-opts [args reqs cli-opts]
  (let [{:keys [options] :as result} (cli/parse-opts args cli-opts)
        missing (set/difference reqs (-> options (keys) (set)))
        missing-errs (map #(format "missing required parameter: %s" (name %))
                          missing)]
    (update result :errors concat missing-errs)))

(defn -main [& args]
  (let [required-args #{:mqtt-host
                        :mqtt-port
                        :mqtt-user
                        :mqtt-password
                        :klaxon-topic
                        }
        {:keys [options _ errors summary]} (parse-opts args required-args cli-opts)]
    (when (:help options) (msg-quit 0 (usage summary)))
    (when (seq errors) (msg-quit 1 (usage summary errors)))
    (let [{:keys [mqtt-host
                  mqtt-port
                  mqtt-user
                  mqtt-password
                  klaxon-topic
                  verbose] options}]
      (when verbose (println (format "launching klaxon server"))))))
