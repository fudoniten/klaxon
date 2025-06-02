(ns klaxon.product
  (:require [clojure.spec.alpha :as s]))

(s/def ::id uuid?)
