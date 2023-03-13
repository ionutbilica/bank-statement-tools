(ns ro.bilica.ionut.tools.finance.statement.classify-summary
  (:require [clojure.string :as str]
            [ro.bilica.ionut.tools.finance.statement.classify :as c]
            [ro.bilica.ionut.tools.finance.statement.classify-save-load :as sl]
            [ro.bilica.ionut.tools.finance.statement.lang-util :refer :all]
            )
  )

(defn all-ancestors[category]
  (let [parts (str/split category #"\.")]
    (for [i (range 1 (inc (count parts)))]
      (str/join "." (take i parts)))))

(defn add-in-summary [s category amount]
  (update s category #(+ amount (zero-if-nil %))))

(defn update-summary [s transaction]
  (reduce #(add-in-summary %1 %2 (Double/parseDouble(::c/amount transaction)))
          s
          (all-ancestors (::c/category transaction))))

(defn summarize-classification [in out]
  (let [transactions (sl/load-classified-transactions in)
        summary (reduce update-summary {} transactions)
        ]
    (sl/save-classification-summary summary out)))