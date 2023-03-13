(ns ro.bilica.ionut.tools.finance.statement.normalize-commons
  (:import (java.text DecimalFormat)
           (java.time LocalDate LocalDateTime)
           (java.time.format DateTimeFormatter)))

(def amount-format (DecimalFormat. "0.00"))
(defn format-amount [amount] (.format amount-format amount))
(defn format-amount-no-decimals [amount] (.format (DecimalFormat. "#") amount))

(def normal-date-format (DateTimeFormatter/ofPattern "dd-MM-yyyy"))
(defn normalize-date [date-str original-format] (.format (.toLocalDate date-str original-format) normal-date-format))
(defn normalize-date-time [date-str original-format] (.format (.toLocalDate (LocalDateTime/parse date-str original-format)) normal-date-format))
(defn parse-normal-date [date-str] (LocalDate/parse date-str normal-date-format))

(defn- reduce-for-balance [r [_ _ amount-str :as t]]
  (let [amount (Double/parseDouble amount-str)
        balance (+ amount (::prev r))
        formatted-balance (format-amount balance)
        ]
    {::prev balance
     ::transactions (conj (::transactions r) (conj (into [] t) formatted-balance))
     }))

(defn add-balance [transactions initial-amount]
  (::transactions (reduce reduce-for-balance {::prev initial-amount ::transactions []} transactions)))

(defn add-csv-header [transactions] (cons ["Date" "Account" "Amount" "Details" "Balance"] transactions))