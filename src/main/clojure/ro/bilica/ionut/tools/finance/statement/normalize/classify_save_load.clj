(ns ro.bilica.ionut.tools.finance.statement.normalize.classify-save-load
  (:require [ro.bilica.ionut.tools.finance.statement.normalize.csv-util :refer :all])
  (:import (java.io File)))

(require '(ro.bilica.ionut.tools.finance.statement.normalize [classify :as c]))

(defn- save-permanent-rules [rules] (write-csv (map #(vector (::c/category %) (::c/token %)) rules) "permanent-rules.csv"))
(defn- save-one-time-rules [rules] (write-csv (map #(vector (::c/category %) (::c/token %) (::c/date %)) rules) "one-time-rules.csv"))

(defn- load-permanent-rules []
  (into [] (map #(assoc {} ::c/category (first %) ::c/token (second %))
                (take-csv (File. "permanent-rules.csv")))))

(defn- load-one-time-rules []
  (into [] (map #(assoc {} ::c/category (first %) ::c/token (second %) ::c/date (nth % 2))
                (take-csv (File. "one-time-rules.csv")))))

(defn load-rules[] {::c/permanent-rules (load-permanent-rules) ::c/one-time-rules (load-one-time-rules)})

(defn save-rules [r]
  (save-permanent-rules (::c/permanent-rules r))
  (save-one-time-rules (::c/one-time-rules r))
  )

(defn classified-transaction-csv-line [{::c/keys [transaction rule]}]
  (into [] (conj (vals transaction) (::c/category rule))))

(defn save-classified-transactions [classified-transactions]
  (write-csv
    (cons ["Category" "Date" "Amount" "Account" "Details" "Balance"]
          (map classified-transaction-csv-line classified-transactions)) "classified-transactions.csv"))

(defn load-normalized-transactions [inputFile]
  (let [lines (rest (take-csv inputFile))]
    (map #(zipmap [::c/date ::c/account ::c/amount ::c/details ::c/balance] %) lines)))
