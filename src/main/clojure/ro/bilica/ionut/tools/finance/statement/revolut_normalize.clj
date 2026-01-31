(ns ro.bilica.ionut.tools.finance.statement.revolut-normalize
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [ro.bilica.ionut.tools.finance.statement.normalize-commons :as common]
            [ro.bilica.ionut.tools.finance.statement.csv-util :as csv-util]
            )
  (:import (java.io File)
           (java.time.format DateTimeFormatter)))

(defn read-input-file [file]
  (with-open [reader (io/reader file)]
    (doall (csv/read-csv reader :separator \,)))) ;TODO why not take-csv

(defn is-revolut-csv? [file]
  (let [file-name (.getName file)]
    (and (s/ends-with? file-name ".csv")
         (s/starts-with? file-name "account-statement_"))))

(def revolut-format (DateTimeFormatter/ofPattern "yyyy-MM-dd H:mm:ss"))

(defn to-ron [amount] (* 4.99 amount))

(defn normalize-transaction [[_ _ date _ desc amount fee currency status]]
  (let [n-date (common/normalize-date-time date revolut-format)
        n-fee (Double/parseDouble fee)
        n-amount (- (Double/parseDouble amount) n-fee)
        n-amount (if (= status "REVERTED") 0 n-amount)
        n-amount-ron (if (= "EUR" currency) (to-ron n-amount) n-amount)
        n-amount-formatted (common/format-amount n-amount-ron)
        n-desc (s/trim desc)
        account (str "revolut-" currency)
        ]
    [n-date account n-amount-formatted n-desc]))

(defn compute-output-file [input-file] (File. (.getParentFile input-file) (str "Normalized-" (.getName input-file))))

(def initial-amounts {"revolut-RON" (+ 61.74 173.61) "revolut-EUR" (to-ron (- 61 61))})

(defn normalize-file [file]
  (let [input-csv (read-input-file file)
        transactions (map #(normalize-transaction %) (rest input-csv))
        with-balance (common/add-balance transactions initial-amounts)
        csv (common/add-csv-header with-balance)
        output-file (compute-output-file file)
        _ (println output-file)
        ]
    (csv-util/write-csv csv output-file)))

(defn normalize-dir [^File dir]
  (let [files (.listFiles dir)]
    (map normalize-file files)))

(defn do-normalize-dir [dir]
  (try
    (doall (normalize-dir dir))
    (finally (shutdown-agents)))
  )

(defn -main [^String input-dir]
  (let [dir (File. input-dir)
        _ (when (not (.exists dir)) (throw (IllegalArgumentException. "Directory not found")))]
    (try
      (doall (normalize-dir dir))
      (finally (shutdown-agents)))
    ))