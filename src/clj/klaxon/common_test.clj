(ns klaxon.common-test
  (:require [clojure.test :refer :all]
            [klaxon.common :as common]))

(deftest test-round-to-dollar
  (testing "Round number to nearest dollar"
    (is (= 10M (common/round-to-dollar 10.49)))
    (is (= 11M (common/round-to-dollar 10.50)))))

(deftest test-round-to-cent
  (testing "Round number to nearest cent"
    (is (= 10.49M (common/round-to-cent 10.494)))
    (is (= 10.50M (common/round-to-cent 10.505)))))
