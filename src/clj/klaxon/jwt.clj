(ns klaxon.jwt
  ;; This namespace handles JWT creation and management.
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [clojure.java.io :as io]

            [fudo-clojure.http.request :as req])

  (:import [com.nimbusds.jose JWSHeader$Builder JWSAlgorithm JOSEObjectType]
           [com.nimbusds.jose.crypto ECDSASigner]
           [com.nimbusds.jwt SignedJWT JWTClaimsSet$Builder]

           [org.bouncycastle.openssl PEMParser]
           [org.bouncycastle.openssl.jcajce JcaPEMKeyConverter]
           [org.bouncycastle.jce.provider BouncyCastleProvider]

           [java.io StringReader]
           [java.security Security KeyFactory PrivateKey]
           [java.security.spec PKCS8EncodedKeySpec]
           [java.security.interfaces ECPrivateKey]
           [java.time Instant]))

(defn- ec-private-key? [o] (instance? ECPrivateKey o))

(s/def ::key-name string?)
(s/def ::key ec-private-key?)

(s/def ::key-data (s/keys :req [::key-name ::key]))

(defn current-timestamp [] (.getEpochSecond (Instant/now)))

(s/fdef load-ec-private-key
  :args (s/cat :pem-str string?)
  :ret  ec-private-key?)
(defn- load-ec-private-key [^String pem-str]
  (Security/addProvider (BouncyCastleProvider.))
  (with-open [reader (PEMParser. (StringReader. pem-str))]
    (let [obj (.readObject reader)
          converter (-> (JcaPEMKeyConverter.) (.setProvider "BC"))
          key (cond
                (instance? PrivateKey obj)
                obj

                (instance? org.bouncycastle.openssl.PEMKeyPair obj)
                (.getPrivateKey converter (.getPrivateKeyInfo obj))

                :else
                (throw (ex-info "Unexpected key type" {:obj obj})))
          kf (KeyFactory/getInstance "EC")]
      (.generatePrivate kf (PKCS8EncodedKeySpec. (.getEncoded key))))))

(s/fdef load-key
  :args (s/cat :key-name string? :key string?)
  :ret  ::key-data)
(defn load-key [key-name key]
  {::key-name key-name
   ::key      (load-ec-private-key key)})

(s/fdef load-key-file
  :args (s/cat :filename string?)
  :ret  ::key-data)
(defn load-key-file
  ;; Loads a private key from a file for JWT signing.
  [filename]
  (when (not (.exists (io/file filename)))
    (throw (ex-info (format "key file does not exist: %s" filename)
                    {:filename filename})))
  (with-open [file (io/reader filename)]
    (let [{key-name :key-name key-data :key-data}
          (json/read file :key-fn keyword)]
      (when (or (nil? key-name) (nil? key-data))
        (throw (ex-info "unable to find :key-name and :key-data, badly formatted key-file" {})))
      (load-key key-name key-data))))

(s/fdef get-jwt!
  :args (s/cat :key-data ::key-data
               :hostname string?
               :method   ::req/http-method
               :url      string?)
  :ret  string?)
(defn get-jwt!
  [{key-name ::key-name key ::key} method hostname path]
  (let [now (current-timestamp)
        uri (format "%s %s%s" (name method) hostname path)
        claims (-> (JWTClaimsSet$Builder.)
                   (.claim "iss" "cdp")
                   (.claim "nbf" now)
                   (.claim "exp" (+ now 120))
                   (.claim "sub" key-name)
                   (.claim "uri" uri)
                   (.build))
        header (-> (JWSHeader$Builder. JWSAlgorithm/ES256)
                   (.type JOSEObjectType/JWT)
                   (.keyID key-name)
                   (.customParam "nonce" (str now))
                   (.build))
        jwt (SignedJWT. header claims)]
    (.sign jwt (ECDSASigner. ^ECPrivateKey key))
    (.serialize jwt)))

(s/fdef authenticate-request!
  :args (s/cat :key-data ::key-data
               :req      ::req/request)
  :ret  ::req/request)
(defn authenticate-request!
  [key req]
  (let [method (req/get-http-method req)
        hostname (req/get-host req)
        path (req/get-base-path req)
        jwt (get-jwt! key method hostname path)]
    (req/with-header req "Authorization"
      (format "Bearer %s" jwt))))
