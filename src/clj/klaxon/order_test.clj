(ns klaxon.order-test
  (:require [clojure.test :refer :all]
            [klaxon.order :as order]))

(deftest test-instantiate-order
  (testing "Instantiate order with valid data"
    (let [order-data {:order_id "123e4567-e89b-12d3-a456-426614174000"
                      :filled_size "10.5"
                      :filled_value "105.0"
                      :fee "0.5"
                      :completion_percentage "100"
                      :product_id "BTC-USD"
                      :last_fill_time "2025-06-05T12:00:00Z"
                      :created_time "2025-06-05T11:00:00Z"
                      :average_filled_price "10.0"
                      :settled true
                      :total_value_after_fees "104.5"
                      :total_fees "0.5"
                      :product_type "btc-usd"
                      :status "filled"
                      :side "buy"
                      :user_id "123e4567-e89b-12d3-a456-426614174001"
                      :order_type "stop-triggered"}]
      (is (order/order? (order/instantiate-order order-data))))))

(deftest test-sale?
  (testing "Check if order is a sale"
    (let [order {:side :sell}]
      (is (order/sale? order)))))

(deftest test-buy?
  (testing "Check if order is a buy"
    (let [order {:side :buy}]
      (is (order/buy? order)))))
