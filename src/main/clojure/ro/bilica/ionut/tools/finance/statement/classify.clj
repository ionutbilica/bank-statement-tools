(ns ro.bilica.ionut.tools.finance.statement.classify
  (:require [clojure.string :as str]
            [ro.bilica.ionut.tools.finance.statement.csv-util :refer :all]
            [ro.bilica.ionut.tools.finance.statement.lang-util :refer :all]
            )
  (:import (clojure.lang Keyword)
           (java.io File Writer)))

(defn matches [str token] (str/includes? (.toLowerCase str) token))

(defn read-token[] (println "token:") (.toLowerCase (read-line)))

(defn one-time-token? [token] (str/starts-with? token "1"))

(defn read-token-until-matches [details]
  (loop [token (read-token)]
    (if (or (matches details token)
            (= "q" token)
            (and (one-time-token? token ) (matches details (subs token 1))))
      token
      (recur (read-token)))))

(defn get-new-rule [t]
  (println t)
  (let [token (read-token-until-matches (::details t))]
    (if (= "q" token) ::quit
                      (let [category (do (println "category:") (read-line))
                            date (when (one-time-token? token ) (::date t))
                            clean-token (if (one-time-token? token) (subs token 1) token)
                            ]
                        {::token clean-token ::category category ::date date}))))

(require '(ro.bilica.ionut.tools.finance.statement [classify-save-load :as sl]))

(defn add-new-rule [rule rules permanent-rules-file one-time-rules-file]
  (let [new-rules (cond (= ::quit rule) (assoc rules ::quit true)
                        (::date rule) (update rules ::one-time-rules conj rule)
                        :else (update rules ::permanent-rules conj rule))]
    (sl/save-rules new-rules permanent-rules-file one-time-rules-file)
    new-rules))

(defn permanent-rule-matches? [details rule] (matches details (::token rule)))
(defn one-time-rule-matches? [transaction rule] (and (matches (::details transaction) (::token rule)) (= (::date transaction) (::date rule))))

(defn find-matching-rules [transaction rules]
  (let [{::keys [permanent-rules one-time-rules]} rules
         {::keys [details]} transaction
         matching-permanent-rules (filter #(permanent-rule-matches? details %) permanent-rules)
         matching-one-time-rules (filter #(one-time-rule-matches? transaction %) one-time-rules)
         matching-rules (concat matching-permanent-rules matching-one-time-rules)
         _ (when (more-than-one? matching-rules) (throw (Exception. (str "Too many rules matching transaction " transaction (into [] matching-rules)))))]
        matching-rules))

(defn add-rule-if-needed [{::keys [rules permanent-rules-file one-time-rules-file] :as r} transaction]
  (if (::quit rules)
    r
    (let [matching-rules (find-matching-rules transaction rules)
          ]
      (if (empty? matching-rules)
        (update r ::rules #(add-new-rule (get-new-rule transaction) % permanent-rules-file one-time-rules-file))
        (do (println "category:" (::category (first matching-rules)) "transaction:" transaction) (println)
            r)))))

(defn classify-transaction [rules transaction]
  (let [matching-rules (find-matching-rules transaction rules)
        ;_ (when (empty? matching-rules) (throw (Exception. (str "No rule matching transaction " transaction))))
        rule (first matching-rules)
        ]
    {::transaction transaction ::rule rule}))

(defn classify [inputFile out permanent-rules-file one-time-rules-file]
  (let [transactions (sl/load-normalized-transactions inputFile)
        existing-rules (sl/load-rules permanent-rules-file one-time-rules-file)
        r (reduce add-rule-if-needed
                      (zipmap [::rules ::permanent-rules-file ::one-time-rules-file] [existing-rules permanent-rules-file one-time-rules-file])
                      transactions)
        _ (sl/save-rules (::rules r) permanent-rules-file one-time-rules-file)
        classified-transactions (map #(classify-transaction (::rules r) %) transactions)
        ]
    (sl/save-classified-transactions classified-transactions out)
  ))

(defn -main [in out permanent-rules-file one-time-rules-file]
  (classify in out permanent-rules-file one-time-rules-file))

(set! *print-namespace-maps* false)
(defmethod print-method Keyword [^Keyword k, ^Writer w]
  (if (.getNamespace k)
    (.write w (str "::" (name k)))
    (.write w (str k))))