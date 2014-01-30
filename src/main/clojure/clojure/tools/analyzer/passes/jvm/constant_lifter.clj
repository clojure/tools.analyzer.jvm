(ns clojure.tools.analyzer.passes.jvm.constant-lifter
  (:require [clojure.tools.analyzer.passes.constant-lifter :as orig]
            [clojure.tools.analyzer :refer [-analyze]]
            [clojure.tools.analyzer.utils :refer [constant? classify]]))

(defn constant-lift
  [{:keys [op var env form] :as ast}]
  (if (= :var op)
    (if (constant? var)
      (let [val @var]
        (assoc (-analyze :const val env (classify val))
          :form form))
      ast)
    (orig/constant-lift ast)))
