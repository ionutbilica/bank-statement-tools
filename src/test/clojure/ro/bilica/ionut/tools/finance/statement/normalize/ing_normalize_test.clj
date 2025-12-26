(ns ro.bilica.ionut.tools.finance.statement.normalize.ing-normalize-test
  (:require [clojure.test :refer :all]
            [ro.bilica.ionut.tools.finance.statement.ing-normalize :as ing]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:import (java.io File)))

(deftest- ing-normalize
          (let [input (File. "src/test/resources/ing-normalize/in")
                actual (ing/normalize-dir input 0)
                expected-location "src/test/resources/ing-normalize/expected-out.csv"
                expected (with-open [reader (io/reader expected-location)]
                           (doall (csv/read-csv reader)))]
            (is (= (count expected) (count actual)) "Actual has expected number of lines")
            (doall (map #(is (= %1 %2)) expected actual))))

(run-all-tests)