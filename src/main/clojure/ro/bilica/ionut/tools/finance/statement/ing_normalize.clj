(ns ro.bilica.ionut.tools.finance.statement.ing-normalize
  (:gen-class)
  (:require [clojure.string :as str]
            [ro.bilica.ionut.tools.finance.statement.csv-util :as csv-util]
            [ro.bilica.ionut.tools.finance.statement.normalize-commons :as common])
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
                             "Sef Serviciu Relatii Clienti"
                             "Şef Serviciu Dezvoltare Produse"
                             "Sef Serviciu Dezvoltare Produse"
                             "ING Bank N.V. Amsterdam"
                             "Sucursala Bucureşti"
                             "Informatii despre schema de garantare"
                             "Sold initial:"
                             "Sold iniţial:"
                             "Sold final"
                             ]) lines))

(defn header? [line] (str/starts-with? line ",Data,,"))

(defn- to-one-transaction-per-line [lines]
  (letfn [(reducer [result line]
            (if (and (str/starts-with? line ",") (not (header? line)))
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
      (str/replace #"\s\s+" "  ")))

(def headers ["Data", "Detalii tranzactie", "Debit", "Credit"])

(defn- parse-headers-to-positions [line]
  (let [positions (map #(.indexOf (str/split (subs line 1) #",") %) headers)]
    (zipmap headers positions)))

(defn- parse-line [line headers-to-positions]
  (let [cells (str/split line #"(,)(?=(?:[^\"]|\"[^\"]*\")*$)") ; https://www.regextester.com/107780
        [date first-details debit-q credit-q] (map #(nth cells (headers-to-positions %)) headers)
        [debitStr creditStr] [(remove-quotes debit-q) (remove-quotes credit-q)]
        [debit credit] [(parse-amount debitStr) (parse-amount creditStr)]
        amount (common/format-amount (- credit debit))
        details (format-details (str/join " " (conj (nthrest cells 9) first-details))) ; Details start at token 9.
        ]
    {:date date :account "ing" :amount amount :details details}))

(defn- parse [r line]
  (println "line" line)
  (if (header? line)
    (assoc r :headers-to-positions (parse-headers-to-positions line))
    (update r :lines conj (parse-line line (:headers-to-positions r)))))

(def ing-format (DateTimeFormatter/ofPattern "dd MMMM yyyy" (Locale/forLanguageTag "ro")))

(defn- reformat-date [line]
  (println line)
  (update line :date #(.format (LocalDate/parse % ing-format) common/normal-date-format)))

(defn- normalize [lines]
  (->> lines
       (remove-non-transactions)
       (to-one-transaction-per-line)
       (reduce parse {})
       :lines
       (map reformat-date)
       (map vals)))

(defn is-csv? [file] (str/ends-with? (.getName file) ".csv"))

(defn normalize-dir [dir initial-amount]
  (let [files (filter is-csv? (.listFiles dir))
        transactions (apply concat (map #(normalize (read-file %)) files))
        sorted-transactions (sort-by #(.toEpochDay (LocalDate/parse (first %) common/normal-date-format)) transactions)
        with-balance (common/add-balance sorted-transactions {"ing" initial-amount})
        csv-content (common/add-csv-header with-balance)
        ]
    csv-content))

(defn ing-normalize-dir [^String input-dir initial-amount out]
  (csv-util/write-csv (normalize-dir (File. input-dir) initial-amount) out)
  (shutdown-agents))