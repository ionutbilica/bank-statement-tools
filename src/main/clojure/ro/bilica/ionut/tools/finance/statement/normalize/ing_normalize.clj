(ns ro.bilica.ionut.tools.finance.statement.normalize.ing-normalize
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:import (java.text DecimalFormat)
           (java.util Locale)
           (java.time LocalDate)
           (java.time.format DateTimeFormatter)
           (java.io File)))

(defn- read-file [f] (str/split-lines (slurp f)))

(defn- remove-headers [lines] (remove #(str/starts-with? % ",Data,,") lines))

(defn- includes-any? [s & [[substr & more]]]
  (when (some? substr)
    (or (str/includes? s substr)
        (recur s [more]))))

(defn- remove-non-transactions [lines]
  (remove #(includes-any? % ["Titular cont: "
                             "Roxana Petria"
                             "Alexandra Ilie"
                             "Şef Serviciu Relatii Clienti"
                             "Şef Serviciu Dezvoltare Produse"
                             "ING Bank N.V. Amsterdam"
                             "Sucursala Bucureşti"
                             "Informatii despre schema de garantare"
                             "Sold initial:"
                             "Sold final"
                             ]) lines))

(defn- to-one-transaction-per-line [lines]
  (letfn [(reducer [result line]
            (if (str/starts-with? line ",")
              (conj (into [] (butlast result)) (str (last result) line))
              (conj result line)))]
    (reduce reducer [] lines)))

(defn- remove-quotes [s] (if (empty? s) "" (subs s 1 (dec (count s)))))

(defn- parse-amount [amount-str]
  (let [amount-str-safe (if (str/blank? amount-str) "0" amount-str)
        amount-str-clean (-> amount-str-safe (.replace "." "") (.replace "," "."))
        ]
    (Double/parseDouble amount-str-clean)))

(def amount-format (DecimalFormat. "0.00"))
(defn- format-amount [amount] (.format amount-format amount))

(defn- reformat-line [line]
  (let [cells (str/split line #"(,)(?=(?:[^\"]|\"[^\"]*\")*$)") ; https://www.regextester.com/107780
        [date _ _ _ _ _ debitQ _ creditQ] cells
        [debitStr creditStr] [(remove-quotes debitQ) (remove-quotes creditQ)]
        [debit credit] [(parse-amount debitStr) (parse-amount creditStr)]
        amount (format-amount (- credit debit))
        details (str/trim (str/join " " (nthrest cells 9))) ; Details start at token 9.
        ]
    {:date date :account "ing" :amount amount :details details}))

(def ing-format (DateTimeFormatter/ofPattern "dd MMMM yyyy" (Locale/forLanguageTag "ro")))
(def out-format (DateTimeFormatter/ofPattern "dd-MM-yyyy"))

(defn- reformat-date [line] (update line :date #(.format (LocalDate/parse % ing-format) out-format)))

(defn- normalize [lines]
  (->> lines
       (remove-headers)
       (remove-non-transactions)
       (to-one-transaction-per-line)
       (map reformat-line)
       (map reformat-date)
       (map vals)))

(defn is-csv? [file] (str/ends-with? (.getName file) ".csv"))

(defn- reduce-for-balance [r [_ _ amount-str :as t]]
  (let [amount (Double/parseDouble amount-str)
        balance (+ amount (::prev r))
        formatted-balance (format-amount balance)
        ]
    {::prev balance
     ::transactions (conj (::transactions r) (conj (into [] t) formatted-balance))
     }))
(defn- add-balance [transactions initial-amount]
  (::transactions (reduce reduce-for-balance {::prev initial-amount ::transactions []} transactions)))

(defn normalize-dir [dir initial-amount]
  (let [files (filter is-csv? (.listFiles dir))
        transactions (apply concat (pmap #(normalize (read-file %)) files))
        sorted-transactions (sort-by #(.toEpochDay (LocalDate/parse (first %) out-format)) transactions)
        with-balance (add-balance sorted-transactions initial-amount)
        csv-content (cons ["Date" "Account" "Amount" "Details" "Balance"] with-balance)
        ]
    csv-content)
  )

(defn- write-csv [lines]
  (with-open [writer (io/writer "ing.csv")]
    (csv/write-csv writer lines)))

(defn -main [input-dir initial-amount]
  (write-csv (normalize-dir (File. input-dir) (Double/parseDouble initial-amount)))
  (shutdown-agents))