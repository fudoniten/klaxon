(ns klaxon.user
  ;; This namespace defines user-related specifications.
  (:require [clojure.spec.alpha :as s]))

(s/def ::id uuid?)
