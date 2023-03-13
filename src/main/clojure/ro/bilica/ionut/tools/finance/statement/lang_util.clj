(ns ro.bilica.ionut.tools.finance.statement.lang-util
  )

(def more-than-one? next)
(defn zero-if-nil [x] (if (nil? x) 0 x))