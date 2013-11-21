(ns clojure.tools.analyzer.jvm.passes-test
  (:refer-clojure :exclude [macroexpand-1])
  (:require [clojure.tools.analyzer.passes :refer :all]
            [clojure.test :refer [deftest is]]
            [clojure.set :as set]
            [clojure.tools.analyzer.jvm.core-test :refer [ast e]]
            [clojure.tools.analyzer.passes.jvm.emit-form
             :refer [emit-form emit-hygienic-form]]
            [clojure.tools.analyzer.passes.jvm.validate :refer [validate]]
            [clojure.tools.analyzer.passes.jvm.clear-locals :refer [clear-locals]]
            [clojure.tools.analyzer.passes.jvm.annotate-branch :refer [annotate-branch]]
            [clojure.tools.analyzer.passes.jvm.analyze-host-expr :refer [analyze-host-expr]]))

(deftest emit-form-test
  (is (= '(monitor-enter 1) (emit-form (ast (monitor-enter 1)))))
  (is (= '(monitor-exit 1) (emit-form (ast (monitor-exit 1)))))
  (is (= '(clojure.core/import* "java.lang.String")
         (emit-form (validate (ast (clojure.core/import* "java.lang.String"))))))
  (is (= '(var clojure.core/+) (emit-form (ast #'+)))))

(deftest annotate-branch-test
  (let [i-ast (annotate-branch (ast (if 1 2 3)))]
    (is (:branch? i-ast))
    (is (= true (-> i-ast :test :test?)))
    (is (= true (-> i-ast :then :path?)))
    (is (= true (-> i-ast :else :path?))))

  (let [fn-ast (prewalk (ast (fn ([]) ([x]))) annotate-branch)]
    (is (every? :path? (-> fn-ast :methods))))

  (let [r-ast (prewalk (ast (reify Object (toString [this] x))) annotate-branch)]
    (is (every? :path? (-> r-ast :methods))))

  (let [l-ast (prewalk (ast (letfn [(x [])] x)) annotate-branch)]
    (is (= true (-> l-ast :body :ret :should-not-clear))))

  (let [c-ast (-> (ast (case 1 0 0 2 2 1)) :body :ret (prewalk annotate-branch))]
    (is (:branch? c-ast))
    (is (= true (-> c-ast :test :test?)))
    (is (= true (-> c-ast :default :path?)))
    (is (every? :path? (-> c-ast :thens)))))

(deftest clear-locals-test
  (let [f-expr (-> (ast (fn [x] (if x x x) x (if x (do x x) (if x x x))))
                 (prewalk annotate-branch)

                 clear-locals :methods first :body)]
    (is (= true (-> f-expr :statements first :then :to-clear? nil?)))
    (is (= true (-> f-expr :statements first :else :to-clear? nil?)))
    (is (= true (-> f-expr :statements second :to-clear? nil?)))
    (is (= true (-> f-expr :ret :then :statements first :to-clear? nil?)))
    (is (= true (-> f-expr :ret :then :ret :to-clear?)))
    (is (= true (-> f-expr :ret :else :then :to-clear?)))
    (is (= true (-> f-expr :ret :else :else :to-clear?)))))
