(ns klaxon.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [klaxon.core :as core]
            [klaxon.order :as order]))

(deftest test-delayed-fill?
  (testing "Check if order is delayed fill"
    (let [threshold-age (java.time.Duration/ofMinutes 20)
          now (java.time.Instant/now)]
      (testing "Order within 20 minutes should not be identified as delayed"
        (let [order {::order/created-time (.minusSeconds now 1199) ;; 19 minutes and 59 seconds ago
                     ::order/filled-size 0
                     ::order/order-type :stop-triggered}]
          (is (not (core/delayed-fill? threshold-age order)))))

      (testing "Order exceeding 20 minutes should be identified as delayed"
        (let [order {::order/created-time (.minusSeconds now 1201) ;; 20 minutes and 1 second ago
                     ::order/filled-size 0
                     ::order/order-type :stop-triggered}]
          (is (core/delayed-fill? threshold-age order)))))))
