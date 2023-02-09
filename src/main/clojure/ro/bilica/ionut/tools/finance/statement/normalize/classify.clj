(ns ro.bilica.ionut.tools.finance.statement.normalize.classify
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File)))

(defn take-csv [f]
  (with-open [fileReader (io/reader f)]
    (doall (csv/read-csv fileReader))))

(defn matches [str token] (str/includes? (.toLowerCase str) token))

(defn read-token[] (println "token:") (.toLowerCase (read-line)))

(defn read-token-until-matches [details]
  (loop [token (read-token)]
    (if (or (matches details token) (= "q" token))
      token
      (recur (read-token)))))

(defn get-new-rule [t]
  (println t)
  (let [token (read-token-until-matches (::details t))]
    (if (= "q" token) ::quit ;will be a switch when oneTime is added.
                (let [category (do (println "categ:") (read-line))]
                  {::token token ::category category}))))

(defn add-new-rule [rule r]
  (if (= ::quit rule) (assoc r ::quit true)
    (assoc r ::permanentRules (conj (r ::permanentRules) rule)))
  )

(defn classify-line [r line]
  (println r)
  (println line)
  (if (::quit r)
    r
    (let [{::keys [transactions permanentRules oneTimeRules]} r
          t (zipmap [::date ::account ::amount ::details ::balance] line)
          details (::details t)
          matchingPermanentRules (filter #(matches details (::token %)) permanentRules)
          matchingPermanentRulesCount (count matchingPermanentRules)
          _ (println (count matchingPermanentRules))]
      (cond
        (= 1 matchingPermanentRulesCount)
          (update r ::transactions conj {::transaction t ::rule (first matchingPermanentRules)})
        (< 1 matchingPermanentRulesCount)
          (throw (Exception. (str "Too many rules matching transaction " line matchingPermanentRules)))
        :else (add-new-rule (get-new-rule t) r)
        )
      )
    )
  )

(defn- write-csv [lines] ;this needs to be moved to a util namespace.
  (println lines)
  (with-open [writer (io/writer "permanent-rules.csv")]
    (csv/write-csv writer lines)))

(defn save-permanent-rules [prs]
  (write-csv (map #(vector (::category %) (::token %)) prs))
  )

(defn load-permanent-rules []
  (into [] (map #(assoc {} ::category (first %) ::token (second %))
                (take-csv (File. "permanent-rules.csv")))))

(defn save [r]
  (save-permanent-rules (::permanentRules r))
  )

(defn classify [inputFile]
  (let [lines (rest (take-csv inputFile))
        r (reduce classify-line {::transactions [] ::permanentRules (load-permanent-rules) ::oneTimeRules []} lines)]
    (save r)
  ))

;(defmacro create-map [& syms]
;  (println (str *ns*))
;  (zipmap (map
;            #(keyword (str *ns*) ^clojure.lang.Symbol %)
;            syms)
;          syms))

(defn -main [& args]
  (classify (File. (first args))))
;(defn -main [& args]
;  (println (let [x 1 y 2 z 3] (create-map x y z))))
