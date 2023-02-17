(ns ro.bilica.ionut.tools.finance.statement.classify-save-load
  (:require [ro.bilica.ionut.tools.finance.statement.csv-util :refer :all]))

(require '(ro.bilica.ionut.tools.finance.statement [classify :as c]))

(defn- save-permanent-rules [rules f] (write-csv (map #(vector (::c/category %) (::c/token %)) rules) f))
(defn- save-one-time-rules [rules f] (write-csv (map #(vector (::c/category %) (::c/token %) (::c/date %)) rules) f))

(defn- load-permanent-rules [f]
  (into [] (map #(assoc {} ::c/category (first %) ::c/token (second %))
                (take-csv f))))

(defn- load-one-time-rules [f]
  (into [] (map #(assoc {} ::c/category (first %) ::c/token (second %) ::c/date (nth % 2))
                (take-csv f))))

(defn load-rules[permanent-rules-file one-time-rules-file] {::c/permanent-rules (load-permanent-rules permanent-rules-file) ::c/one-time-rules (load-one-time-rules one-time-rules-file)})

(defn save-rules [r permanent-rules-file one-time-rules-file]
  (save-permanent-rules (::c/permanent-rules r) permanent-rules-file)
  (save-one-time-rules (::c/one-time-rules r) one-time-rules-file)
  )

(defn classified-transaction-csv-line [{::c/keys [transaction rule]}]
  (into [] (conj (vals transaction) (::c/category rule "nocat"))))

(defn save-classified-transactions [classified-transactions out]
  (write-csv
    (cons ["Category" "Date" "Amount" "Account" "Details" "Balance"]
          (map classified-transaction-csv-line classified-transactions)) out))

(defn load-normalized-transactions [inputFile]
  (let [lines (rest (take-csv inputFile))]
    (map #(zipmap [::c/date ::c/account ::c/amount ::c/details ::c/balance] %) lines)))
