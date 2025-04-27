(ns klaxon.core
  (:require [clojure.string :as str]
            [clojure.data.json :as json]

            [buddy.core.codecs :as codecs]
            [buddy.core.mac :as mac]

            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]

            [klaxon.types :as t])

  (:import java.time.Instant
           java.nio.charset.Charset
           java.util.Base64))

(def signature-algo :hmac+sha256)

(defn- current-epoch-timestamp []
  (.getEpochSecond (Instant/now)))

(s/fdef to-path-element
  :args (s/cat :el any?)
  :ret  string?)
(defn- to-path-element [el]
  (cond (keyword? el) (name el)
        (uuid? el)    (.toString el)
        (string? el)  el))

(s/fdef build-path
  :args (s/cat :path-elements (s/* string?))
  :ret  string?)
(defn- build-path
  [& path-elements]
  (str "/" (str/join "/" (map to-path-element path-elements))))

(s/fdef string->bytes
  :args (s/cat :str string?)
  :ret  ::t/bytes)
(defn- string->bytes [str] (.getBytes str))

(s/fdef bytes->string
  :args (s/cat :bytes ::t/bytes)
  :ret  string?)
(defn- bytes->string [bytes]
  (String. bytes (Charset/forName "UTF-8")))

(s/fdef sign-message
  :args (s/cat :key ::t/bytes :msg string?)
  :ret  ::t/base64-string)
(defn- sign-message [key msg]
  (-> (mac/hash msg {:key key :alg signature-algo})
      (codecs/bytes->b64)
      (bytes->string)))

(stest/instrument `sign-message)

(s/fdef make-signer
  :args (s/cat :key ::t/bytes)
  :ret  string?)

;; Goal: Check the status of current orders, and find any over some threshold.
;; If any orders over that threshold are completed, page me.
