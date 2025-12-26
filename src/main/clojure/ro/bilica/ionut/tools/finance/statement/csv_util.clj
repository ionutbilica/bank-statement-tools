(ns ro.bilica.ionut.tools.finance.statement.csv-util
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  )

(defn take-csv
  ([f]
   (if (.exists (io/as-file f))
     (with-open [fileReader (io/reader f)]
       (doall (csv/read-csv fileReader)))
     []))
  ([f & keys] (map #(zipmap keys %) (take-csv f)))
  )

(defn take-csv-without-header
  ([f] (rest (take-csv f)))
  ([f & keys] (rest (apply take-csv f keys)))
  )

(defn write-csv [lines f]
  (io/make-parents f)
  (with-open [writer (io/writer f)]
    (csv/write-csv writer lines)))

(defn ceva
  ([f & keys] (map #(zipmap keys %) '([1 2] [3 4]))))

(defn- -main[]
  (println (ceva 'x' ::a ::b))
  )