(ns ro.bilica.ionut.tools.finance.statement.revolut-normalize
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [ro.bilica.ionut.tools.finance.statement.normalize.normalize-commons :as common]
            [ro.bilica.ionut.tools.finance.statement.normalize.csv-util :as csv-util]
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

(def revolut-format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))

(defn normalize-transaction [[_ _ date _ desc amount] account]
  (let [n-date (common/normalize-date date revolut-format)
        n-amount (common/format-amount (Double/parseDouble amount))
        n-desc (s/trim desc)
        ]
    [n-date account n-amount n-desc]))

(defn parse-account [csv]
  (let [currency (nth (nth csv 1) 7)]
    (str "revolut-" currency)))

(defn compute-output-file [input-file] (File. (.getParentFile input-file) (str "Normalized-" (.getName input-file))))

(defn to-ron [amount] (* 4.95 amount))
(defn transaction-to-ron [t] (update t 2 #(common/format-amount(to-ron (Double/parseDouble %))))) ;TODO stop formatting and parsing!!!

(def initial-amounts {"revolut-RON" (+ 81.76 39) "revolut-EUR" (to-ron (+ 14.74 9.99))})

(defn normalize-file [file]
  (let [input-csv (read-input-file file)
        account (parse-account input-csv)
        transactions (map #(normalize-transaction % account) (rest input-csv))
        in-ron (if (= account "revolut-EUR") (map transaction-to-ron transactions) transactions)
        with-balance (common/add-balance in-ron (initial-amounts account))
        csv (common/add-csv-header with-balance)
        output-file (compute-output-file file)
        ]
    (csv-util/write-csv csv output-file)))

(defn normalize-dir [dir]
  (let [files (filter is-revolut-csv? (.listFiles dir))]
    (pmap normalize-file files)))

(defn -main [^String input-dir]
  (let [dir (File. input-dir)
        _ (when (not (.exists dir)) (throw (IllegalArgumentException. "Directory not found")))]
    (try
      (doall (normalize-dir dir))
      (finally (shutdown-agents)))
    ))