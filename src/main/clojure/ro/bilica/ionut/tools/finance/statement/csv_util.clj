(ns ro.bilica.ionut.tools.finance.statement.csv-util
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  )

(defn take-csv
  ([f]
   (if (.exists f)
     (with-open [fileReader (io/reader f)]
       (doall (csv/read-csv fileReader)))
     []))
  ([f & keys] (map #(zipmap keys %) (take-csv f)))
  )

(defn take-csv-without-header
  ([f] (rest (take-csv f)))
  ([f & keys] (rest (take-csv f keys)))
  )

(defn write-csv [lines f]                                   ;this needs to be moved to a util namespace.
  (with-open [writer (io/writer f)]
    (csv/write-csv writer lines)))
