(ns klaxon.core-test
  (:require [clojure.test :refer :all]
            [klaxon.core :as core]
            [klaxon.order :as order]))

(deftest test-delayed-fill?
  (testing "Check if order is delayed fill"
    (let [threshold-age (java.time.Duration/ofMinutes 20)
          order {:created-time (java.time.Instant/now)
                 :filled-size 0
                 :order-type :stop-triggered}]
      (is (core/delayed-fill? threshold-age order)))))
