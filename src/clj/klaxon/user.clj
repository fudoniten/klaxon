(ns klaxon.user
  (:require [clojure.spec.alpha :as s]))

(s/def ::id uuid?)
