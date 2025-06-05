(ns klaxon.types
  ;; This namespace defines custom types and specifications.
  (:require [clojure.spec.alpha :as s]))

(s/def ::custom-type (s/keys :req [::bytes ::base64-string]))

(s/def ::bytes (fn [x] (instance? (Class/forName "[B") x)))

(def base64-regex #"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")

(s/def ::base64-string
  (s/and string? #(re-matches base64-regex %)))
