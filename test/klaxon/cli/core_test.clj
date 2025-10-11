(ns klaxon.cli.core-test
  (:require [clojure.test :refer :all]
            [klaxon.cli.core :as c]))

(deftest test-parse-opts
  (testing "Parse CLI options with required arguments"
      (let [args ["-k" "keyfile.json" "-t" "topic" "-p" "60"]
            result (#'c/parse-opts args #{:key-file :ntfy-topic :poll-seconds} c/cli-opts)]
      (is (empty? (:errors result))))))
