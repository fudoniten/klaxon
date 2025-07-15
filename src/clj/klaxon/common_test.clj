(ns klaxon.common-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [klaxon.utils :as utils]))

(deftest test-round-to-dollar
  (testing "Round number to nearest dollar"
    (is (= 10M (utils/round-to-dollar 10.49)))
    (is (= 11M (utils/round-to-dollar 10.51)))))

(deftest test-round-to-cent
  (testing "Round number to nearest cent"
    (is (= 10.49M (utils/round-to-cent 10.494)))
    (is (= 10.50M (utils/round-to-cent 10.505)))))

(run-tests)
