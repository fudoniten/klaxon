(ns klaxon.account
  ;; This namespace defines account-related specifications and protocols.
  (:require [clojure.spec.alpha :as s]
            [klaxon.common :as common]))

(s/def ::balance   number?)
(s/def ::hold      number?)
(s/def ::available number?)

(defprotocol CurrencyAccount
  (currency  [self])
  (balance   [self])
  (hold      [self])
  (available [self]))

(def account? (partial satisfies? CurrencyAccount))

(defn currency-account? [curr acct]
  (= curr (currency acct)))

(defn account-balance [accts curr]
  ;; Retrieves the balance for a specific currency account.
  (balance (get accts curr)))

(s/def ::acct account?)

(s/fdef currency
  :args (s/cat :acct ::acct)
  :ret  ::common/currency)

(s/fdef balance
  :args (s/cat :acct ::acct)
  :ret  decimal?)

(s/fdef hold
  :args (s/cat :acct ::acct)
  :ret  decimal?)

(s/fdef available
  :args (s/cat :acct ::acct)
  :ret  decimal?)
