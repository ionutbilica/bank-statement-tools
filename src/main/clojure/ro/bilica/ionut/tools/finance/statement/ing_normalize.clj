(ns ro.bilica.ionut.tools.finance.statement.ing-normalize
  (:gen-class)
  (:require [clojure.string :as str]
            [ro.bilica.ionut.tools.finance.statement.normalize.csv-util :as csv-util]
            [ro.bilica.ionut.tools.finance.statement.normalize.normalize-commons :as common])
  (:import (java.io File)
           (java.time LocalDate)
           (java.time.format DateTimeFormatter)
           (java.util Locale)))

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

(defn- format-details [details]
  (-> details
      (str/trim)
      (str/replace #"\s\s+" "  "))
  )

(defn- reformat-line [line]
  (let [cells (str/split line #"(,)(?=(?:[^\"]|\"[^\"]*\")*$)") ; https://www.regextester.com/107780
        [date _ _ _ _ _ debitQ _ creditQ] cells
        [debitStr creditStr] [(remove-quotes debitQ) (remove-quotes creditQ)]
        [debit credit] [(parse-amount debitStr) (parse-amount creditStr)]
        amount (common/format-amount (- credit debit))
        details (format-details (str/join " " (nthrest cells 9))) ; Details start at token 9.
        ]
    {:date date :account "ing" :amount amount :details details}))

(def ing-format (DateTimeFormatter/ofPattern "dd MMMM yyyy" (Locale/forLanguageTag "ro")))

(defn- reformat-date [line] (update line :date #(.format (LocalDate/parse % ing-format) common/normal-date-format)))

(defn- normalize [lines]
  (->> lines
       (remove-headers)
       (remove-non-transactions)
       (to-one-transaction-per-line)
       (map reformat-line)
       (map reformat-date)
       (map vals)))

(defn is-csv? [file] (str/ends-with? (.getName file) ".csv"))

(defn normalize-dir [dir initial-amount]
  (let [files (filter is-csv? (.listFiles dir))
        transactions (apply concat (pmap #(normalize (read-file %)) files))
        sorted-transactions (sort-by #(.toEpochDay (LocalDate/parse (first %) common/normal-date-format)) transactions)
        with-balance (common/add-balance sorted-transactions initial-amount)
        csv-content (common/add-csv-header with-balance)
        ]
    csv-content))

(defn -main [^String input-dir initial-amount]
  (csv-util/write-csv (normalize-dir (File. input-dir) (Double/parseDouble initial-amount)) "ing.csv")
  (shutdown-agents))