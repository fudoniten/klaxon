(ns klaxon.product
  ;; This namespace defines product-related specifications.
  (:require [clojure.spec.alpha :as s]))

(s/def ::product (s/keys :req [::id]))
