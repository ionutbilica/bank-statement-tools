(ns ro.bilica.ionut.tools.finance.statement.normalize.ing-normalize
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:import (java.util Locale)
           (java.time LocalDate)
           (java.time.format DateTimeFormatter)
           (java.io File)))

(defn- read-file [f] (str/split-lines (slurp f)))

(defn- remove-headers [lines] (remove #(str/starts-with? % ",Data,,") lines))

(defn- includes-any? [s subs]
  (if (empty? subs)
    false
    (if (str/includes? s (first subs))
      true
      (recur s (rest subs)))))

(defn- remove-non-transactions [lines] (remove #(includes-any? % ["Roxana Petria"
                                                                  "Alexandra Ilie"
                                                                  "Sef Serviciu Relatii Clienti"
                                                                  "Sef Serviciu Dezvoltare Produse"
                                                                  "ING Bank N.V. Amsterdam - Sucursala Bucuresti"
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

(defn- reformat-line [line]
  (let [cells (str/split line #"(,)(?=(?:[^\"]|\"[^\"]*\")*$)") ; https://www.regextester.com/107780
        [date _ _ _ _ debitQ _ creditQ] cells
        [debit credit] [(remove-quotes debitQ) (remove-quotes creditQ)]
        details (str/trim (str/join " " (nthrest cells 8))) ; Details start at token 8.
        ]
    {:date date :account "ing" :debit debit :credit credit :details details})) ; TODO There should be a nicer way but I don't remember.

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

(defn normalize-dir [dir]
  (let [files (filter is-csv? (.listFiles dir))
        transactions (apply concat (pmap #(normalize (read-file %)) files))
        csv-content (cons ["Date" "Account" "Debit" "Credit" "Details"] transactions)
        ]
    csv-content)
  )

(defn- write-csv [lines]
  (with-open [writer (io/writer "ing.csv")]
    (csv/write-csv writer lines)))

(defn -main [& args]
    (write-csv (normalize-dir (File. (first args))))
  (shutdown-agents))