(ns ro.bilica.ionut.tools.finance.statement.main
  (:require [ro.bilica.ionut.tools.finance.statement.ing-normalize :as ing_normalize]
            [ro.bilica.ionut.tools.finance.statement.revolut-normalize :as revolut-normalize]
            [ro.bilica.ionut.tools.finance.statement.merge :as merge]
            [ro.bilica.ionut.tools.finance.statement.classify :as classify]
            [ro.bilica.ionut.tools.finance.statement.classify-summary :as sum]
            )
  (:import (java.io File))
  )

;(def root "D:\\resources\\raport2024\\ala\\")
(def root "D:\\resources\\raport2025\\ala\\")
(defn -main []
  #_(ing_normalize/ing-normalize-dir
           (str root "ing-raw")
           3631.14
           (str root "normalized\\ing.csv"))
  #_(revolut-normalize/do-normalize-dir
           (File. (str root "revolut-raw")))
  #_(merge/merge-csvs
    (str root "merged.csv")
    (str root "normalized\\ing.csv")
    (str root "normalized\\revolut.csv")
    ;(str root "normalized\\revolut-eur.csv")
    )
  (classify/classify
    (str root "merged.csv")
    (str root "classified.csv")
    (str root "..//permanent-rules.csv")
    (str root "..//one-time-rules.csv")
    )
  (sum/summarize-classification
    (str root "classified.csv")
    (str root "summarized.csv")
    )
  )
