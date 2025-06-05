(ns klaxon.account
  ;; This namespace defines account-related specifications and protocols.
  (:require [clojure.spec.alpha :as s]))

(s/def ::balance   number?)
(s/def ::hold      number?)
(s/def ::available number?)

(defprotocol CurrencyAccount
  (currency  [self])
  (balance   [self])
  (hold      [self])
  (available [self]))

(def account? (partial satisfies? CurrencyAccount))

(s/def ::account (s/keys :req [::balance ::hold ::available]))

(defn currency-account? [curr acct]
  (= curr (currency acct)))

(defn account-balance [accts curr]
  ;; Retrieves the balance for a specific currency account.
  (balance (get accts curr)))

(s/def ::acct account?)


(s/fdef balance
  :args (s/cat :acct ::acct)
  :ret  decimal?)

(s/fdef hold
  :args (s/cat :acct ::acct)
  :ret  decimal?)

(s/fdef available
  :args (s/cat :acct ::acct)
  :ret  decimal?)
