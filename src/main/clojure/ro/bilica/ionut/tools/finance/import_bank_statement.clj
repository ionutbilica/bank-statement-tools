(ns ro.bilica.ionut.tools.finance.import-bank-statement
      (:gen-class)
      (:require [clojure.string :as str])
      (:import (java.util Locale)
               (java.time LocalDate)
               (java.time.format DateTimeFormatter)))

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


(defn- reformat-line [line]
  (let [cells (str/split line #"(,)(?=(?:[^\"]|\"[^\"]*\")*$)") ; https://www.regextester.com/107780
        [date _ _ _ _ debit _ credit] cells
        details (str/trim (str/join " " (nthrest cells 8)))
        ]
    {:date date :debit debit :credit credit :details details})) ; TODO Is there a nicer way?

(def ing-format (DateTimeFormatter/ofPattern "dd MMMM yyyy" (Locale/forLanguageTag "ro")))
(def out-format (DateTimeFormatter/ofPattern "dd-MM-yyyy"))

(defn- reformat-date [line] (update line :date #(.format (LocalDate/parse % ing-format) out-format)))

(defn- clean [lines]
  (->> lines
       (remove-headers)
       (remove-non-transactions)
       (to-one-transaction-per-line)
       (map reformat-line)
       (map reformat-date)
       ))

(defn -main [& args]
  (println "----" (apply str (interpose "\n" (clean (read-file (first args)))))))