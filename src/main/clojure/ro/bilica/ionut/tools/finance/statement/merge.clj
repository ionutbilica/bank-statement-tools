(ns ro.bilica.ionut.tools.finance.statement.merge
  (:require [ro.bilica.ionut.tools.finance.statement.csv-util :as csv-util])
  (:require [ro.bilica.ionut.tools.finance.statement.normalize-commons :as normalize-commons])
  )

(defn load-transactions [files]
  (mapcat #(csv-util/take-csv-without-header % ::date ::account ::amount ::details ::balance) files))

(defn countme[c] (println (count c)) c)

(defn merge-csvs [out & ins]
  (csv-util/write-csv (->> ins
                          load-transactions
                           countme
                          (sort-by #(normalize-commons/parse-normal-date (::date %)))
                           (map vals)
                           )
                      out)
  )

(defn -main [name & files]
  )
