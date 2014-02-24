(ns clojure.tools.analyzer.jvm.passes-test
  (:refer-clojure :exclude [macroexpand-1])
  (:require [clojure.tools.analyzer.ast :refer :all]
            [clojure.test :refer [deftest is]]
            [clojure.set :as set]
            [clojure.tools.analyzer.passes.add-binding-atom :refer [add-binding-atom]]
            [clojure.tools.analyzer.passes.collect :refer [collect-closed-overs]]
            [clojure.tools.analyzer.jvm.core-test :refer [ast ast1 e f f1]]
            [clojure.tools.analyzer.passes.jvm.emit-form
             :refer [emit-form emit-hygienic-form]]
            [clojure.tools.analyzer.passes.jvm.validate :refer [validate]]
            [clojure.tools.analyzer.passes.jvm.annotate-tag :refer [annotate-tag]]
            [clojure.tools.analyzer.passes.jvm.clear-locals :refer [clear-locals]]
            [clojure.tools.analyzer.passes.jvm.infer-tag :refer [infer-tag]]
            [clojure.tools.analyzer.passes.jvm.annotate-branch :refer [annotate-branch]]
            [clojure.tools.analyzer.passes.jvm.annotate-methods :refer [annotate-methods]]
            [clojure.tools.analyzer.passes.jvm.annotate-loops :refer [annotate-loops]]
            [clojure.tools.analyzer.passes.jvm.fix-case-test :refer [fix-case-test]]
            [clojure.tools.analyzer.passes.jvm.analyze-host-expr :refer [analyze-host-expr]]
            [clojure.tools.analyzer.passes.jvm.classify-invoke :refer [classify-invoke]])
  (:import (clojure.lang Keyword Var Symbol AFunction
                         PersistentVector PersistentArrayMap PersistentHashSet ISeq)
           java.util.regex.Pattern))

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
    (is (= true (-> f-expr :ret :else :else :to-clear?))))
  (let [f-expr (-> (ast (fn [x] (loop [a x] (if 1 x (do x (recur x))))))
                 (prewalk (comp annotate-branch annotate-loops))
                 (collect-closed-overs {:what  #{:closed-overs}
                                        :where #{:fn :loop}
                                        :top-level? false})
                 clear-locals :methods first :body :ret)]
    (is (= true (-> f-expr :bindings first :init :to-clear? nil?)))
    (is (= true (-> f-expr :body :ret :then :to-clear?)))
    (is (= true (-> f-expr :body :ret :else :statements first :to-clear? nil?)))
    (is (= true (-> f-expr :body :ret :else :ret :exprs first :to-clear? nil?))))
  (let [f-expr (-> (ast (loop [] (do (let [a 1] (loop [] a)) (recur))))
                 (prewalk (comp annotate-branch annotate-loops))
                 (collect-closed-overs {:what  #{:closed-overs}
                                        :where #{:loop}
                                        :top-level? false})
                 clear-locals
                 :body :ret :statements first :body :ret :body :ret)]
    (is (= true (-> f-expr :to-clear?))))
  (let [f-expr (-> (ast (loop [] (do (let [a 1] (loop [] (if 1 a (recur)))) (recur))))
                 (prewalk (comp annotate-branch annotate-loops))
                 (collect-closed-overs {:what  #{:closed-overs}
                                        :where #{:loop}
                                        :top-level? false})
                 clear-locals
                 :body :ret :statements first :body :ret :body :ret :then)]
    (is (= true (-> f-expr :to-clear?)))))

(deftest fix-case-test-test
  (let [c-ast (-> (ast (case 1 1 1)) add-binding-atom (prewalk fix-case-test))]
    (is (= true (-> c-ast :body :ret :test :atom deref :case-test)))))

(deftest annotate-tag-test
  (is (= PersistentVector (-> {:op :const :form [] :val []} annotate-tag :tag)))
  (is (= PersistentVector (-> (ast []) annotate-tag :tag)))
  (is (= PersistentVector (-> (ast '[]) annotate-tag :tag)))
  (is (= PersistentArrayMap(-> (ast {}) annotate-tag :tag)))
  (is (= PersistentHashSet (-> (ast #{}) annotate-tag :tag)))
  (is (= ISeq (-> (ast '()) annotate-tag :tag)))
  (is (= Class (-> {:op :const :type :class :form Object :val Object}
                 annotate-tag :tag)))
  (is (= String (-> (ast "foo") annotate-tag :tag)))
  (is (= Keyword (-> (ast :foo) annotate-tag :tag)))
  (is (= Character/TYPE (-> (ast \f) annotate-tag :tag)))
  (is (= Long/TYPE (-> (ast 1) annotate-tag :tag)))
  (is (= Pattern (-> (ast #"foo") annotate-tag :tag)))
  (is (= Var (-> (ast #'+)  annotate-tag :tag)))
  (is (= Boolean (-> (ast true) annotate-tag :tag)))
  (let [b-ast (-> (ast (let [a 1] a)) add-binding-atom
                 (postwalk annotate-tag))]
    (is (= Long/TYPE (-> b-ast :body :ret :tag)))))

(deftest classify-invoke-test
  (is (= :keyword-invoke (-> (ast (:foo {})) classify-invoke :op)))
  (is (= :protocol-invoke (-> (ast (f nil)) classify-invoke :op)))
  (is (= :instance? (-> (ast (instance? String ""))
                      (prewalk validate) classify-invoke :op)))
  (is (= :prim-invoke (-> (ast (f1 1)) (prewalk infer-tag) classify-invoke :op))))

(deftest annotate-methods-test
  (let [r-ast (-> (ast (reify Object (toString [_] ""))) (prewalk annotate-methods))]
    (is (= 'toString (-> r-ast :expr :methods first :name)))
    (is (= [] (-> r-ast :expr :methods first :params)))
    (is (= '_ (-> r-ast :expr :methods first :this :name)))))

;; TODO: test primitives, tag matching, throwing validation, method validation
(deftest validate-test
  (is (= String (-> (ast String) validate :val)))
  (is (= 'String (-> (ast String) validate :form)))
  (is (= Exception (-> (ast (try (catch Exception e)))
                     (prewalk validate) :catches first :class)))
  (is (-> (ast (set! *warn-on-reflection* true)) validate))
  (is (= true (-> (ast (String. "foo")) (postwalk annotate-tag) validate
              :validated?)))

  (let [s-ast (-> (ast (Integer/parseInt "7")) (prewalk annotate-tag) analyze-host-expr validate)]
    (is (:validated? s-ast))
    (is (= Integer/TYPE (:tag s-ast)))
    (is (= [String] (mapv :tag (:args s-ast)))))

  (let [i-ast (-> (ast (.hashCode "7")) (prewalk annotate-tag) analyze-host-expr validate)]
    (is (:validated? i-ast))
    (is (= Integer/TYPE (:tag i-ast)))
    (is (= [] (mapv :tag (:args i-ast))))
    (is (= String (:class i-ast))))

  (is (= true (-> (ast (import java.lang.String)) (prewalk validate) :ret :validated?))))

;; we need all or most those passes to perform those tests
(deftest all-passes-test
  (let [t-ast (ast1 (let [a 1
                          b 2
                          c (str a)
                          d (Integer/parseInt c b)]
                      (Integer/getInteger c d)))]
    (is (= Integer (-> t-ast :body :tag)))
    (is (= Integer (-> t-ast :tag)))
    (is (= Long/TYPE (->> t-ast :bindings (filter #(= 'a (:form %))) first :tag)))
    (is (= String (->> t-ast :bindings (filter #(= 'c (:form %))) first :tag)))
    (is (= Integer/TYPE (->> t-ast :bindings (filter #(= 'd (:form %))) first :tag))))
  (is (= Void/TYPE (:tag (ast1 (.println System/out "foo")))))

  (let [d-ast (ast1 (Double/isInfinite 2))]
    (is (= Boolean/TYPE (-> d-ast :tag)))
    (is (= Double/TYPE (->> d-ast :args first :tag)))))
