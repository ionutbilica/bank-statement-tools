(ns ro.bilica.ionut.tools.finance.statement.merge
  (:require [ro.bilica.ionut.tools.finance.statement.normalize.csv-util :as csv-util])
  )

(defn load-transactions [files]
  (mapcat #(csv-util/take-csv-without-header % ::date ::account ::amount ::details ::balance) files))

(defn merge [out & ins]
  (csv-util/write-csv (-> ins
                          load-transactions
                          (sort-by ::date)
                          (map vals))
                      (str name ".csv"))
  )

(defn -main [name & files]
  )
