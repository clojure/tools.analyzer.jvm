;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.jvm
  "Analyzer for clojure code, extends tools.analyzer with JVM specific passes/forms"
  (:refer-clojure :exclude [macroexpand-1 macroexpand])
  (:require [clojure.tools.analyzer
             :as ana
             :refer [analyze analyze-in-env wrapping-meta analyze-fn-method]
             :rename {analyze -analyze}]
            [clojure.tools.analyzer.utils :refer [ctx maybe-var]]
            [clojure.tools.analyzer.passes :refer [walk prewalk postwalk cycling]]
            [clojure.tools.analyzer.jvm.utils :refer :all :exclude [box]]
            [clojure.tools.analyzer.passes.source-info :refer [source-info]]
            [clojure.tools.analyzer.passes.cleanup :refer [cleanup1 cleanup2]]
            [clojure.tools.analyzer.passes.elide-meta :refer [elide-meta]]
            [clojure.tools.analyzer.passes.constant-lifter :refer [constant-lift]]
            [clojure.tools.analyzer.passes.warn-earmuff :refer [warn-earmuff]]
            [clojure.tools.analyzer.passes.collect :refer [collect]]
            [clojure.tools.analyzer.passes.add-binding-atom :refer [add-binding-atom]]
            [clojure.tools.analyzer.passes.uniquify :refer [uniquify-locals]]
            [clojure.tools.analyzer.passes.jvm.box :refer [box]]
            [clojure.tools.analyzer.passes.jvm.annotate-branch :refer [annotate-branch]]
            [clojure.tools.analyzer.passes.jvm.annotate-methods :refer [annotate-methods]]
            [clojure.tools.analyzer.passes.jvm.fix-case-test :refer [fix-case-test]]
            [clojure.tools.analyzer.passes.jvm.clear-locals :refer [clear-locals]]
            [clojure.tools.analyzer.passes.jvm.classify-invoke :refer [classify-invoke]]
            [clojure.tools.analyzer.passes.jvm.validate :refer [validate]]
            [clojure.tools.analyzer.passes.jvm.infer-tag :refer [infer-tag]]
            [clojure.tools.analyzer.passes.jvm.annotate-tag :refer [annotate-literal-tag annotate-binding-tag]]
            [clojure.tools.analyzer.passes.jvm.validate-loop-locals :refer [validate-loop-locals]]
            [clojure.tools.analyzer.passes.jvm.analyze-host-expr :refer [analyze-host-expr]]))

(def specials
  "Set of the special forms for clojure in the JVM"
  (into ana/specials
        '#{var monitor-enter monitor-exit clojure.core/import* reify* deftype* case*}))

(defmulti parse
  "Extension to tools.analyzer/-parse for JVM special forms"
  (fn [[op & rest] env] op))

(defmethod parse :default
  [form env]
  (ana/-parse form env))

(defn empty-env
  "Returns an empty env map"
  []
  {:context :expr :locals {} :ns (ns-name *ns*)
   :namespaces (atom
                ;; TODO: reify IPersistentMap so that it reflects runtime edits
                (into {} (mapv #(vector (ns-name %)
                                        {:mappings (ns-map %)
                                         :aliases  (reduce-kv (fn [a k v] (assoc a k (ns-name v)))
                                                              {} (ns-aliases %))
                                         :ns       (ns-name %)})
                               (all-ns))))})

(defn desugar-host-expr [form env]
  (cond
   (symbol? form)
   (let [target (maybe-class (namespace form))
         field (symbol (name form))]
     (if (and (namespace form) target)
       (with-meta (list '. target field)
         (merge (meta form)
                {:field true})) ;; should use this
       form))

   (seq? form)
   (let [[op & expr] form]
     (if (symbol? op)
       (let [opname (name op)]
         (cond

          (= (first opname) \.) ; (.foo bar ..)
          (let [[target & args] expr
                target (if-let [target (and (not (get (:locals env) target))
                                            (maybe-class target))]
                         (with-meta (list 'clojure.core/identity target) {:tag Class})
                         target)
                args (list* (symbol (subs opname 1)) args)]
            (with-meta (list '. target (if (= 1 (count args)) ;; we don't know if (.foo bar) is
                                         (first args) args)) ;; a method call or a field access
              (meta form)))

          (and (namespace op)
               (maybe-class (namespace op))) ; (class/field ..)
          (let [target (maybe-class (namespace op))]
            (with-meta (list '. target (list* (symbol opname) expr)) ;; static access in call position however are always method calls
              (meta form)))

          (= (last opname) \.) ;; (class. ..)
          (with-meta (list* 'new (symbol (subs opname 0 (dec (count opname)))) expr)
            (meta form))

          :else form))
       form))

   :else form))

(defn macroexpand-1
  "If form represents a macro form or an inlineable function,
   returns its expansion, else returns form."
  [form env]
  (if (seq? form)
    (let [op (first form)]
      (if (specials op)
        form
        (let [v (maybe-var op env)
              m (meta v)
              local? (-> env :locals (get op))
              macro? (and (not local?) (:macro m))
              inline-arities-f (:inline-arities m)
              args (rest form)
              inline? (and (not local?)
                           (or (not inline-arities-f)
                               (inline-arities-f (count args)))
                           (:inline m))]
          (cond

           macro?
           (apply v form env (rest form)) ; (m &form &env & args)

           inline?
           (vary-meta (apply inline? args) merge m)

           :else
           (desugar-host-expr form env)))))
    (desugar-host-expr form env)))

(defn create-var
  "Creates a Var for sym and returns it.
   The Var gets interned in the env namespace."
  [sym {:keys [ns]}]
  (intern ns sym))

(defmethod parse 'var
  [[_ var :as form] env]
  (if-let [var (maybe-var var env)]
    {:op   :the-var
     :env  env
     :form form
     :var  var}
    (throw (ex-info (str "var not found: " var) {:var var}))))

(defmethod parse 'monitor-enter
  [[_ target :as form] env]
  {:op       :monitor-enter
   :env      env
   :form     form
   :target   (-analyze target (ctx env :expr))
   :children [:target]})

(defmethod parse 'monitor-exit
  [[_ target :as form] env]
  {:op       :monitor-exit
   :env      env
   :form     form
   :target   (-analyze target (ctx env :expr))
   :children [:target]})

(defmethod parse 'clojure.core/import*
  [[_ class :as form] env]
  {:op    :import
   :env   env
   :form  form
   :class class})

(defn analyze-method-impls
  [[name [this & params :as args] & body :as form] env]
  {:pre [(symbol? name)
         (vector? args)
         this]}
  (let [meth (cons params body)
        this-expr {:name  this
                   :env   env
                   :form  this
                   :op    :binding
                   :tag   (:this env)
                   :local :this}
        env (assoc-in (dissoc env :this) [:locals this] this-expr)
        method (analyze-fn-method meth env)]
    (assoc (dissoc method :variadic?)
      :op       :method
      :form     form
      :this     this-expr
      :name     (symbol (clojure.core/name name))
      :children (into [:this] (:children method)))))

(defn -deftype [name class-name args interfaces]
  (let [interfaces (mapv #(symbol (.getName ^Class %)) interfaces)]
    (eval (list 'do (list 'deftype* name class-name args :implements interfaces)
                (list 'import class-name)))))

(defmethod parse 'reify*
  [[_ interfaces & methods :as form] env]
  (let [interfaces (conj (disj (set (mapv maybe-class interfaces)) Object)
                         clojure.lang.IObj)
        name (gensym "reify__")
        class-name (symbol (str (namespace-munge *ns*) "$" name))
        menv (assoc env :this class-name)
        methods (mapv #(assoc (analyze-method-impls % menv) :interfaces interfaces)
                      methods)]

    (-deftype name class-name [] interfaces)

    (wrapping-meta
     {:op         :reify
      :env        env
      :form       form
      :class-name class-name
      :methods    methods
      :interfaces interfaces
      :children   [:methods]})))

(defmethod parse 'deftype*
  [[_ name class-name fields _ interfaces & methods :as form] env]
  (let [interfaces (disj (set (mapv maybe-class interfaces)) Object)
        fields-expr (mapv (fn [name]
                            {:env     env
                             :form    name
                             :name    name
                             :mutable (let [m (meta name)]
                                        (or (and (:unsynchronized-mutable m)
                                                 :unsynchronized-mutable)
                                            (and (:volatile-mutable m)
                                                 :volatile-mutable)))
                             :local   :field
                             :op      :binding})
                          fields)
        menv (assoc env
               :context :expr
               :locals (zipmap fields fields-expr)
               :this class-name)
        methods (mapv #(assoc (analyze-method-impls % menv) :interfaces interfaces)
                      methods)]

    (-deftype name class-name fields interfaces)

    {:op         :deftype
     :env        env
     :form       form
     :name       name
     :class-name class-name
     :fields     fields-expr
     :methods    methods
     :interfaces interfaces
     :children   [:fields :methods]}))

(defmethod parse 'case*
  [[_ expr shift mask default case-map switch-type test-type & [skip-check?] :as form] env]
  (let [[low high] ((juxt first last) (keys case-map))
        test-expr (-analyze expr (ctx env :expr))
        [tests thens] (reduce (fn [[te th] [min-hash [test then]]]
                                (let [test-expr (-analyze (list 'quote test) env)
                                      then-expr (-analyze then env)]
                                  [(conj te {:op       :case-test
                                             :hash     min-hash
                                             :test     test-expr
                                             :children [:test]})
                                   (conj th {:op       :case-then
                                             :hash     min-hash
                                             :then     then-expr
                                             :children [:then]})]))
                              [[] []] case-map) ;; transform back in a sorted-map + hash-map when emitting
        default-expr (-analyze default env)]
    {:op          :case
     :form        form
     :env         env
     :test        (assoc test-expr :case-test true)
     :default     default-expr
     :tests       tests
     :thens       thens
     :shift       shift
     :mask        mask
     :low         low
     :high        high
     :switch-type switch-type
     :test-type   test-type
     :skip-check? skip-check?
     :children    [:test :tests :thens :default]}))


(defmethod parse 'catch
  [[_ etype ename & body :as form] env]
  (let [etype (if (= etype :default) Throwable etype)]
    (ana/-parse `(catch ~etype ~ename ~@body) env)))

(defn analyze
  "Returns an AST for the form that's compatible with what tools.emitter.jvm requires.

   Binds tools.analyzer/{macroexpand-1,create-var,parse} to
   tools.analyzer.jvm/{macroexpand-1,create-var,parse} and calls
   tools.analyzer/analyzer on form.

   Applies the following passes in the correct order to the returning AST:
   * uniquify
   * add-binding-atom
   * cleanup
   * source-info
   * elide-meta
   * constant-lifter
   * warn-earmuff
   * collect
   * jvm.box
   * jvm.annotate-branch
   * jvm.annotate-methods
   * jvm.fix-case-test
   * jvm.clear-locals
   * jvm.classify-invoke
   * jvm.validate
   * jvm.infer-tag
   * jvm.annotate-tag
   * jvm.validate-loop-locals
   * jvm.analyze-host-expr"
  [form env]
  (binding [ana/macroexpand-1 macroexpand-1
            ana/create-var    create-var
            ana/parse         parse]
    (-> (-analyze form env)

      (prewalk cleanup1)

      uniquify-locals
      add-binding-atom

      (walk (fn [ast]
              (-> ast
                warn-earmuff
                annotate-branch
                source-info
                elide-meta
                annotate-methods
                fix-case-test))
            constant-lift)

      ((fn analyze [ast]
         (-> ast
           (postwalk
            (comp (cycling infer-tag analyze-host-expr annotate-binding-tag
                        validate classify-invoke)
               annotate-literal-tag)) ;; not necesary, select on v-l-l
           (prewalk
            (comp box
               (validate-loop-locals analyze)))))) ;; empty binding atom

      (prewalk
       (comp cleanup2
          (collect :constants
                   :callsites
                   :closed-overs)))

      clear-locals)))
