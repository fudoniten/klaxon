(ns klaxon.user
  ;; This namespace defines user-related specifications.
  (:require [clojure.spec.alpha :as s]))

(s/def ::user (s/keys :req [::id]))
(ns klaxon.utils
  ;; This namespace contains general utility functions.
  (:require [clojure.string :as str])
  (:import java.math.RoundingMode
           [java.time Duration Instant]))

(defn round-to-dollar [n]
  ;; Rounds a number to the nearest dollar.
  (.setScale (bigdec n) 0 RoundingMode/HALF_EVEN))

(defn round-to-cent [n]
  (.setScale (bigdec n) 2 RoundingMode/HALF_EVEN))

(defn current-epoch-timestamp []
  (.getEpochSecond (Instant/now)))

(defn current-timestamp []
  (Instant/now))

(defn instant-to-epoch-timestamp [instant]
  (.getEpochSecond instant))

(defn parse-epoch-timestamp [epoch-second]
  (Instant/ofEpochSecond epoch-second))

(defn parse-timestamp [timestamp]
  (Instant/parse timestamp))

(defn string->bytes [s] (.getBytes s))
(defn bytes->string [bs]
  (String. bs (java.nio.charset.Charset/forName "UTF-8")))

(defn base64-encode [to-encode]
  ;; Encodes a byte array to a Base64 string.
  (.encode (java.util.Base64/getEncoder)
           to-encode))

(defn base64-encode-string [to-encode]
  (.encodeToString (java.util.Base64/getEncoder)
           to-encode))

(defn base64-decode [to-decode]
  (.decode (java.util.Base64/getDecoder)
           to-decode))
