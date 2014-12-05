(ns clojure.tools.analyzer.jvm.passes-test
  (:refer-clojure :exclude [macroexpand-1])
  (:require [clojure.tools.analyzer.ast :refer :all]
            [clojure.tools.analyzer.jvm :as ana.jvm]
            [clojure.tools.analyzer.env :as env]
            [clojure.tools.analyzer.passes :refer [schedule]]
            [clojure.test :refer [deftest is]]
            [clojure.set :as set]
            [clojure.tools.analyzer.passes.add-binding-atom :refer [add-binding-atom]]
            [clojure.tools.analyzer.passes.collect-closed-overs :refer [collect-closed-overs]]
            [clojure.tools.analyzer.jvm.core-test :refer [ast ast1 e f f1]]
            [clojure.tools.analyzer.passes.jvm.emit-form
             :refer [emit-form emit-hygienic-form]]
            [clojure.tools.analyzer.passes.jvm.validate :as v]
            [clojure.tools.analyzer.passes.jvm.annotate-tag :refer [annotate-tag]]
            [clojure.tools.analyzer.passes.jvm.infer-tag :refer [infer-tag]]
            [clojure.tools.analyzer.passes.jvm.annotate-branch :refer [annotate-branch]]
            [clojure.tools.analyzer.passes.jvm.annotate-host-info :refer [annotate-host-info]]
            [clojure.tools.analyzer.passes.jvm.annotate-loops :refer [annotate-loops]]
            [clojure.tools.analyzer.passes.jvm.fix-case-test :refer [fix-case-test]]
            [clojure.tools.analyzer.passes.jvm.analyze-host-expr :refer [analyze-host-expr]]
            [clojure.tools.analyzer.passes.jvm.classify-invoke :refer [classify-invoke]])
  (:import (clojure.lang Keyword Var Symbol AFunction
                         PersistentVector PersistentArrayMap PersistentHashSet ISeq)
           java.util.regex.Pattern))

(defn validate [ast]
  (env/with-env (ana.jvm/global-env)
    (v/validate ast)))

(deftest emit-form-test
  (is (= '(monitor-enter 1) (emit-form (ast (monitor-enter 1)))))
  (is (= '(monitor-exit 1) (emit-form (ast (monitor-exit 1)))))
  (is (= '(clojure.core/import* "java.lang.String")
         (emit-form (validate (ast (clojure.core/import* "java.lang.String"))))))
  (is (= '(var clojure.core/+) (emit-form (ast #'+))))
  (is (= '(:foo {}) (emit-form (ast (:foo {})))))
  (is (= '(try 1 (catch Exception e nil))
         (emit-form (ana.jvm/analyze '(try 1 (catch Exception e))))))
  (is (= '(try 1 (catch Exception e nil))
         (emit-form (ana.jvm/analyze '(try 1 (catch Exception e)))
                    {:qualifed-symbols true})))
  (is (= '(f [] 1) (emit-form (ast (f [] 1))))))

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

  (let [c-ast (-> (ast (case 1 0 0 2 2 1)) :body :ret (prewalk annotate-branch))]
    (is (:branch? c-ast))
    (is (= true (-> c-ast :test :test?)))
    (is (= true (-> c-ast :default :path?)))
    (is (every? :path? (-> c-ast :thens)))))

(deftest fix-case-test-test
  (let [c-ast (-> (ast (case 1 1 1)) add-binding-atom (prewalk fix-case-test))]
    (is (= true (-> c-ast :body :ret :test :atom deref :case-test)))))

(deftest annotate-tag-test
  (is (= PersistentVector (-> {:op :const :form [] :val []} annotate-tag :tag)))
  (is (= PersistentVector (-> (ast []) annotate-tag :tag)))
  (is (= PersistentArrayMap(-> (ast {}) annotate-tag :tag)))
  (is (= PersistentHashSet (-> (ast #{}) annotate-tag :tag)))
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
  (is (= :invoke (-> (ast (:foo {} 1)) classify-invoke :op)))
  (is (= :protocol-invoke (-> (ast (f nil)) classify-invoke :op)))
  (is (= :instance? (-> (ast (instance? String ""))
                      (prewalk analyze-host-expr) classify-invoke :op)))
  (is (= :prim-invoke (-> (ast (f1 1)) (prewalk infer-tag) classify-invoke :op))))

(deftest annotate-host-info-test
  (let [r-ast (-> (ast ^:foo (reify Object (toString [_] ""))) (prewalk annotate-host-info))]
    (is (= 'toString (-> r-ast :expr :methods first :name)))
    (is (= [] (-> r-ast :expr :methods first :params)))
    (is (= '_ (-> r-ast :expr :methods first :this :name)))))

;; TODO: test primitives, tag matching, throwing validation, method validation
(deftest validate-test
  (is (= Exception (-> (ast (try (catch Exception e)))
                     (prewalk (comp validate analyze-host-expr)) :catches first :class :val)))
  (is (-> (ast (set! *warn-on-reflection* true)) validate))
  (is (= true (-> (ast (String. "foo")) (postwalk (comp validate annotate-tag analyze-host-expr))
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

  (is (= String (-> (ast1 String) :val)))
  (is (= 'String (-> (ast1 String) :form)))
(is (= PersistentVector (-> (ast1 '[]) :tag)))
(is (= ISeq (-> (ast1 '()) :tag)))

  (let [d-ast (ast1 (Double/isInfinite 2))]
    (is (= Boolean/TYPE (-> d-ast :tag)))
    (is (= Double/TYPE (->> d-ast :args first :tag)))))

;; checks for specific bugs that have surfaced
(deftest annotate-case-loop
  (is (ast1 (loop [] (case 1 :a (recur) :b 42)))))

(deftest var-tag-inference
  (let [ast (ana.jvm/analyze '(def a "foo")
                             (ana.jvm/empty-env)
                             {:passes-opts (merge ana.jvm/default-passes-opts
                                                  {:infer-tag/level :global})})]
    (is (= String (-> ast :var meta :tag)))))

(deftest validate-handlers
  ;; test for tanal-24, without the handler analysis would throw
  ;; with an handler that ignores the tag, we can simulate the current behaviour
  ;; of the clojure compiler
  (is (ana.jvm/analyze '(defn ^long a [] 1)
                       (ana.jvm/empty-env)
                       {:passes-opts (merge ana.jvm/default-passes-opts
                                            {:validate/wrong-tag-handler (fn [t ast]
                                                                           {t nil})})})))
