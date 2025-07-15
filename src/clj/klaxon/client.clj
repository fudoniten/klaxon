(ns klaxon.client
  ;; This namespace handles the creation and management of HTTP clients.
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]

            [klaxon.config :as config]
            [klaxon.jwt :as jwt]

            [fudo-clojure.http.client :as http]
            [fudo-clojure.http.request :as req]))

(s/def ::hostname (s/and string? #(= % (config/get-config :hostname))))

(s/def ::client
  (s/keys :req [::hostname ::http-client]))

(s/def ::client (s/keys :req [::hostname ::http-client]))

(s/fdef create
  :args (s/cat :opts (s/keys* :req-un [::hostname ::jwt/key-data]))
  :ret  ::client)
(defn create
  ;; Creates a new HTTP client with authentication.
  [& {:keys [::hostname ::jwt/key-data]}]
  (assert (some? hostname) "hostname cannot be nil")
  (assert (some? key-data) "key-data cannot be nil")
  (try
    (let [authenticator (fn [req] (jwt/authenticate-request! key-data req))
          http-client (http/json-client :authenticator authenticator)]
      {::hostname hostname ::http-client http-client})
    (catch Exception e
      (throw (ex-info "Failed to create HTTP client" {:exception e})))))

(defn- to-path-elem [el]
  (cond (keyword? el) (name el)
        (uuid? el)    (.toString el)
        (string? el)  el
        :else         (throw (ex-info (format "invalid path element: %s" el)
                                      {}))))

(defn build-path [& els]
  ;; Constructs a URL path from given elements.
  (str "/" (str/join "/" (map to-path-elem els))))

(s/fdef get!
  :args (s/cat :client ::client :req ::req/request)
  :ret  any?)
(defn get! [{:keys [::http-client ::hostname]} req]
  (http/get! http-client (-> req (req/with-host hostname))))

(require '[clojure.spec.test.alpha :as stest])
(stest/instrument 'get!)
