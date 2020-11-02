(ns ro.bilica.ionut.tools.finance.statement.normalize.revolut-normalize
  (:gen-class)
  (:require [clojure.string :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File)
           (java.time.format DateTimeFormatter)
           (java.time LocalDate)))

(defn read-input-file [file]
  (with-open [reader (io/reader file)]
    (doall (csv/read-csv reader :separator \;))))

(defn is-revolut-csv? [file]
  (let [file-name (.getName file)]
    (and (s/ends-with? file-name ".csv")
         (s/starts-with? file-name "Revolut-"))))

(def revolut-format (DateTimeFormatter/ofPattern "MMM d, yyyy "))
(def out-format (DateTimeFormatter/ofPattern "dd-MM-yyyy"))

(defn normalize-date [date] (.format (LocalDate/parse date revolut-format) out-format))

(defn normalize-amount [amount] (-> amount (s/trim) (s/replace "." "x") (s/replace "," ".") (s/replace "x" ",")))

(defn normalize-transaction [[date desc debit credit _ _ _ categ notes] account]
  (let [n-date (normalize-date date)
        n-debit (normalize-amount debit)
        n-credit (normalize-amount credit)
        n-desc (str (s/trim desc) " " (s/trim categ) " " (s/trim notes))
        ]
    [n-date account n-debit n-credit n-desc]))

(defn normalize [csv account]
  (cons ["Date" "Account" "Debit" "Credit" "Details"]
        (map #(normalize-transaction % account) (rest csv))))

(defn- write-csv [csv output-file]
  (with-open [writer (io/writer output-file)]
    (csv/write-csv writer csv)))

(defn parse-account [file]
  (let [[revolut currency] (s/split (.getName file) #"-")]
    (str revolut "-" currency)))

(defn compute-output-file [input-file] (File. (.getParentFile input-file) (str/replace (.getName input-file) "Statement" "Normalized")))

(defn normalize-file [file]
  (let [input-csv (read-input-file file)
        account (parse-account file)
        normalized-csv (normalize input-csv account)
        output-file (compute-output-file file)
        ]
    (write-csv normalized-csv output-file)))

(defn normalize-dir [dir]
  (let [files (filter is-revolut-csv? (.listFiles dir))]
    (pmap normalize-file files)))

(defn -main [& args]
  (let [dir (File. (first args))
        _ (when (not (.exists dir)) (throw (IllegalArgumentException. "Directory not found")))]
    (try
      (doall (normalize-dir dir))
      (finally (shutdown-agents)))
    ))