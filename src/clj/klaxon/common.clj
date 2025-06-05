(ns klaxon.common
  ;; This namespace contains common utilities and helper functions.
  (:require [clojure.core.async :refer [>! go-loop timeout chan alt!]]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [klaxon.logging :as logging])
  (:import java.math.RoundingMode
           [java.time Duration Instant]))

(def logger (logging/print-logger))

(defn- instant? [obj]
  (instance? java.time.Instant obj))

(s/def ::timestamp instant?)

(s/def ::amount decimal?)

(s/def ::percentage (s/and decimal? #(<= 0.0 % 100.0)))

(s/def ::duration (partial instance? Duration))

(defn round-to-dollar [n]
  ;; Rounds a number to the nearest dollar.
  (.setScale (bigdec n) 0 RoundingMode/HALF_EVEN))

(defn round-to-cent [n]
  (.setScale (bigdec n) 2 RoundingMode/HALF_EVEN))

(s/def ::currency keyword?)

(defn transform-key [m k new-k f]
  (assoc m new-k (f (get m k))))

(defn key-eq [k v]
  (fn [m]
    (= v (get m k))))
(s/fdef key-eq
  :args (s/cat :k keyword? :m any?)
  :ret  boolean?)

(defmacro *-> [& fs]
  (let [init (gensym)]
    `(fn [~init] (-> ~init ~@fs))))

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
(s/fdef base64-decode
  :args (s/cat :to-decode bytes?)
  :ret  bytes?)

(defn ensure-conform [spec obj]
  (if (s/valid? spec obj)
    obj
    (throw (RuntimeException. (s/explain-str spec obj)))))

(defn to-uuid [s] (java.util.UUID/fromString s))

(defn sort-by-field [field]
  (partial sort-by field (fn [a b] (.before a b))))

(defprotocol Result
  (success? [self])
  (error? [self])
  (result-type [self])
  (map-success [self f])
  (bind [self f])
  (content [self])
  (to-string [self]))

(def result? (partial satisfies? Result))

(s/fdef bind
  :args (s/cat :self result? :f (s/fspec :ret result?))
  :ret  result?)

(defprotocol ResultError
  (message [self])
  (exception [self]))

(defn exception-error [e]
  (reify
    Result
    (error? [_] true)
    (success? [_] false)
    (map-success [self _] self)
    (bind [self _] self)
    (result-type [_] :exception-error)
    (content [_] (throw e))
    (to-string [_] (str "#error[" (.toString e) "]"))

    ResultError
    (message [_] (.toString e))
    (exception [_] e)))

(defmacro catching-errors [& args]
  (let [e (gensym)]
    `(try (do ~@args)
          (catch java.lang.RuntimeException ~e
            (exception-error ~e)))))

(defn success [o]
  (reify
    Result
    (success? [_] true)
    (error? [_] false)
    (map-success [_ f] (f o))
    (bind [_ f] (catching-errors (f o)))
    (result-type [_] :success)
    (content [_] o)
    (to-string [_] (str "#success[" o "]"))))

(defn result-of [spec]
  (s/and result? spec))

(defmacro dispatch-result [result success-fn error-fn]
  (let [success-arg  (first (first success-fn))
        success-body (rest success-fn)
        error-arg    (first (first error-fn))
        error-body   (rest error-fn)]
    `(if (success? ~result)
       (let [~success-arg (content ~result)]
         ~@success-body)
       (let [~error-arg ~result]
         ~@error-body))))

(s/fdef ensure-keys
  :args (s/cat :ks (s/coll-of keyword?) :m (s/map-of keyword? any?))
  :ret  map?)
(defn ensure-keys [ks m]
  (let [diff (set/difference ks (set (keys m)))]
    (when (seq diff)
      (throw (ex-info (str "missing keys: "
                           (str/join "," diff))
                      {:missing-keys diff
                       :map m})))
    m))

(defmacro ->* [& forms]
  (when (empty? forms)
    (throw (IllegalArgumentException. "->* requires at least one form")))
  `(fn [x#]
     (-> x# ~@forms)))

(defn convert-key [m k new-k f]
  (assoc m new-k (f (get m k))))

(defn pthru [o] (pprint o) o)

(defn convert-keys [m & triples]
  (when (not= (mod (count triples) 3) 0)
    (throw (ex-info "convert-keys takes triples of old-key, new-key and conversion function." {})))
  (let [conversions (partition 3 triples)]
    (reduce (fn [m [old new f]] (assoc m new (f (get m old))))
            m
            conversions)))

(defn heartbeat-chan
  [delay-secs]
  (let [heartbeat (chan)
        delay     (atom delay-secs)
        stop      (chan)]
    (go-loop []
      (let [update (alt! (timeout (* @delay 1000)) ([_] {:type :timeout :content (Instant/now)})
                         stop                      ([_] {:type :stop}))]
        (case (:type update)
          :timeout (do (>! heartbeat {:timestamp (:content update)})
                       (recur))
          :stop    (println "stopping heartbeat..."))))
    {:heartbeat heartbeat :delay delay :stop stop}))
