(ns clojure.tools.analyzer.jvm.single-pass
  "Interface to clojure.lang.Compiler/analyze.

  Entry point `analyze-path` and `analyze-one`"
  (:refer-clojure :exclude [macroexpand])
  (:import (java.io LineNumberReader InputStreamReader PushbackReader)
           (clojure.lang RT LineNumberingPushbackReader Compiler$DefExpr Compiler$LocalBinding Compiler$BindingInit Compiler$LetExpr
                         Compiler$LetFnExpr Compiler$StaticMethodExpr Compiler$InstanceMethodExpr Compiler$StaticFieldExpr
                         Compiler$NewExpr Compiler$EmptyExpr Compiler$VectorExpr Compiler$MonitorEnterExpr
                         Compiler$MonitorExitExpr Compiler$ThrowExpr Compiler$InvokeExpr Compiler$TheVarExpr Compiler$VarExpr
                         Compiler$UnresolvedVarExpr Compiler$ObjExpr Compiler$NewInstanceMethod Compiler$FnMethod Compiler$FnExpr
                         Compiler$NewInstanceExpr Compiler$MetaExpr Compiler$BodyExpr Compiler$ImportExpr Compiler$AssignExpr
                         Compiler$TryExpr$CatchClause Compiler$TryExpr Compiler$C Compiler$LocalBindingExpr Compiler$RecurExpr
                         Compiler$MapExpr Compiler$IfExpr Compiler$KeywordInvokeExpr Compiler$InstanceFieldExpr Compiler$InstanceOfExpr
                         Compiler$CaseExpr Compiler$Expr Compiler$SetExpr Compiler$MethodParamExpr 

                         Compiler$LiteralExpr
                         Compiler$KeywordExpr
                         Compiler$ConstantExpr Compiler$NumberExpr Compiler$NilExpr Compiler$BooleanExpr Compiler$StringExpr

                         Compiler$ObjMethod Compiler$Expr))
  (:require [clojure.reflect :as reflect]
            [clojure.java.io :as io]
            [clojure.repl :as repl]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as ana.jvm]
            [clojure.tools.analyzer.passes.jvm.emit-form :as emit-form]
            [clojure.tools.analyzer.passes.uniquify :refer [uniquify-locals]]
            [clojure.tools.analyzer.passes.jvm.infer-tag :refer [infer-tag]]
            [clojure.tools.analyzer.env :as env]
            [clojure.tools.analyzer.jvm :as taj]
            [clojure.tools.analyzer.jvm.utils :as ju]
            [clojure.tools.analyzer.utils :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Interface

(def ^:dynamic *eval-ast* 
  "If true, evaluate the output AST before returning.
  Otherwise, AST is unevaluated. Defaults to false."
  false)

(declare analyze-one)

(defn analyze
  ([form] (analyze form (ana.jvm/empty-env) {}))
  ([form env] (analyze form env {}))
  ([form env opts]
   (env/ensure (ana.jvm/global-env)
     (-> (analyze-one (merge env (select-keys (meta form) [:line :column :ns :file])) form opts)
         uniquify-locals))))

(defn analyze-form
  ([form] (analyze-form form {}))
  ([form opt] (analyze form (ana.jvm/empty-env) opt)))

(defmacro ast
  "Returns the abstract syntax tree representation of the given form,
  evaluated in the current namespace"
  ([form] `(ast ~form {}))
  ([form opt]
   `(analyze-form '~form ~opt)))

;;;;;;;;;;;;;;;;;;;;;;;
;; Utils

(defmacro field
  "Call a private field, must be known at compile time. Throws an error
  if field is already publicly accessible."
  ([class-obj field] `(field ~class-obj ~field nil))
  ([class-obj field obj]
   (let [{class-flags :flags :keys [members]} (reflect/reflect (resolve class-obj))
         {field-flags :flags} (some #(and (= (:name %) field) %) members)]
     (assert field-flags
             (str "Class " (resolve class-obj) " does not have field " field))
     (assert (not (and (:public class-flags)
                       (:public field-flags)))
             (str "Class " (resolve class-obj) " and field " field " is already public")))
   `(field-accessor ~class-obj '~field ~obj)))

(defn- field-accessor [^Class class-obj field obj]
  (let [^java.lang.reflect.Field
        field (.getDeclaredField class-obj (name field))]
    (.setAccessible field true)
    (let [ret (.get field obj)]
      (if (instance? Boolean ret)
        (boolean ret)
        ret))))

(defn- method-accessor [^Class class-obj method obj types & args]
  (let [^java.lang.reflect.Method
        method (.getMethod class-obj (name method) (into-array Class types))]
    (.setAccessible method true)
    (try
      (.invoke method obj (object-array args))
      (catch java.lang.reflect.InvocationTargetException e
        (throw (repl/root-cause e))))))

(defn- when-column-map [expr]
  (let [field (try (.getDeclaredField (class expr) "column")
                (catch Exception e))]
    (when field
      {:column (field-accessor (class expr) 'column expr)})))

(defn- when-line-map [expr]
  (let [^java.lang.reflect.Method
        method (try (.getMethod (class expr) "line" (into-array Class []))
                 (catch Exception e))
        field (try (.getDeclaredField (class expr) "line")
                (catch Exception e))]
    (cond 
      method {:line (method-accessor (class expr) 'line expr [])}
      field {:line (field-accessor (class expr) 'line expr)})))

(defn- when-source-map [expr]
  (let [field (try (.getDeclaredField (class expr) "source")
                (catch Exception e))]
    (when field
      {:file (field-accessor (class expr) 'source expr)})))

(defn- env-location [env expr]
  (merge env
         (when-line-map expr)
         (when-column-map expr)
         ;; only adds the suffix of the path
         #_(when-source-map expr)))

(defn- inherit-env [expr env]
  (merge env
         (when-let [line (-> expr :env :line)]
           {:line line})
         (when-let [column (-> expr :env :column)]
           {:column column})
         (when-let [file (-> expr :env :file)]
           {:file file})))

(defprotocol AnalysisToMap
  (analysis->map [aobj env opt]
    "Recursively converts the output of the Compiler's analysis to a map. Takes
    a map of options:
    - :children
      when true, include a :children key with all child expressions of each node
    - :java-obj
      when true, include a :java-obj key with the node's corresponding Java object"))

;; Literals extending abstract class Compiler$LiteralExpr and have public value fields

;  {:op   :const
;   :doc   "Node for a constant literal or a quoted collection literal"
;   :keys [[:form "A constant literal or a quoted collection literal"]
;          [:literal? "`true`"]
;          [:type "one of :nil, :bool, :keyword, :symbol, :string, :number, :type, :record, :map, :vector, :set, :seq, :char, :regex, :class, :var, or :unknown"]
;          [:val "The value of the constant node"]
;          ^:optional ^:children
;          [:meta "An AST node representing the metadata of the constant value, if present. The node will be either a :map node or a :const node with :type :map"]]}

(defn tag-for-val [val]
  {:post [((some-fn nil? class?) %)]}
  (let [c (ju/unbox (class val))]
    c))

(defmacro literal-dispatch [disp-class op-keyword]
  {:pre [((some-fn nil? keyword?) op-keyword)]}
  `(extend-protocol AnalysisToMap
     ~disp-class
     (analysis->map
       [expr# env# opt#]
       (let [v# (.eval expr#)
             tag# (tag-for-val v#)
                 #_(method-accessor (class expr#) '~'getJavaClass expr# [])]
         {:op :const
          :tag tag#
          :o-tag tag#
          :literal? true
          :type (or ~op-keyword
                    (u/classify v#))
          :env env#
          :val v#
          :form v#}))))

#_(literal-dispatch Compiler$KeywordExpr :keyword)
#_(literal-dispatch Compiler$NumberExpr :number)
#_(literal-dispatch Compiler$StringExpr :string)
#_(literal-dispatch Compiler$NilExpr :nil)
#_(literal-dispatch Compiler$BooleanExpr :bool)

(literal-dispatch Compiler$EmptyExpr nil)

(defn quoted-list? [val]
  (boolean
    (and (list? val)
         (#{2} (count val))
         (#{'quote} (first val)))))

(defn parse-constant [val]
  (cond
    (list? val) (if (or (empty? val)
                        (quoted-list? val))
                  val
                  (list 'quote val))
    (coll? val) (into (empty val)
                      (for [[k v] val]
                        [(parse-constant k)
                         (parse-constant v)]))
    (symbol? val) (list 'quote val)
    :else val))

(extend-protocol AnalysisToMap
  Compiler$LiteralExpr
  (analysis->map
    [expr env opt]
    (let [val (.eval expr)
          ;; t.a.j is much more specific with things like maps. 
          ;; eg. Compiler returns APersistentMap, but t.a.j has PersistentArrayMap
          tag (tag-for-val val)
                #_(method-accessor (class expr) 'getJavaClass expr [])
          inner {:op :const
                 :form val
                 :tag tag
                 :o-tag tag
                 :literal? true
                 :type (u/classify val)
                 :env env
                 :val val}]
      inner))

  ;; a ConstantExpr is always originally quoted.
  Compiler$ConstantExpr
  (analysis->map
    [expr env opt]
    (let [val (.eval expr)
          ; used as :form for emit-form
          ;_ (prn "Constant val" val)
          ;; t.a.j is much more specific with things like maps. 
          ;; eg. Compiler returns APersistentMap, but t.a.j has PersistentArrayMap
          tag (tag-for-val val)
                #_(method-accessor (class expr) 'getJavaClass expr [])
          inner {:op :const
                 :form val
                 :tag tag
                 :o-tag tag
                 :literal? true
                 :type (u/classify val)
                 :env env
                 :val val}]
      {:op :quote
       :form (list 'quote val)
       :literal? true
       :env env
       :tag tag
       :o-tag tag
       :expr inner
       :children [:expr]})))

(extend-protocol AnalysisToMap

  ;; def
  ; {:op   :def
  ;  :doc  "Node for a def special-form expression"
  ;  :keys [[:form "`(def name docstring? init?)`"]
  ;         [:name "The var symbol to define in the current namespace"]
  ;         [:var "The Var object created (or found, if it already existed) named by the symbol :name in the current namespace"]
  ;         ^:optional ^:children
  ;         [:meta "An AST node representing the metadata attached to :name, if present. The node will be either a :map node or a :const node with :type :map"]
  ;         ^:optional ^:children
  ;         [:init "An AST node representing the initial value of the var"]
  ;         ^:optional
  ;         [:doc "The docstring for this var"]]}
  Compiler$DefExpr
  (analysis->map
    [expr env opt]
    (let [env (env-location env expr)
          init? (field Compiler$DefExpr initProvided expr)
          init (analysis->map (field Compiler$DefExpr init expr) env opt)
          meta (when-let [meta (field Compiler$DefExpr meta expr)]
                 (analysis->map meta env opt))
          children (into (into [] (when meta [:meta]))
                         (when init? [:init]))
          ^clojure.lang.Var var (field Compiler$DefExpr var expr)
          name (.sym var)]
      (merge 
        {:op :def
         :form (list* 'def name 
                      (when init?
                        [(emit-form/emit-form init)]))
         :tag clojure.lang.Var
         :o-tag clojure.lang.Var
         :env env
         :name name
         :var var}
        (when init?
          {:init init})
        (when meta
          (assert (= :quote (:op meta)))
          {:meta (:expr meta)})
        (when-not (empty? children)
          {:children children}))))

  ;; let/loop
  ; {:op   :let
  ;  :doc  "Node for a let* special-form expression"
  ;  :keys  [[:form "`(let* [binding*] body*)`"]
  ;          ^:children
  ;          [:bindings "A vector of :binding AST nodes with :local :let"]
  ;          ^:children
  ;          [:body "Synthetic :do node (with :body? `true`) representing the body of the let expression"]]}
  ; {:op   :loop
  ;  :doc  "Node a loop* special-form expression"
  ;  :keys [[:form "`(loop* [binding*] body*)`"]
  ;         ^:children
  ;         [:bindings "A vector of :binding AST nodes with :local :loop"]
  ;         ^:children
  ;         [:body "Synthetic :do node (with :body? `true`) representing the body of the loop expression"]
  ;         [:loop-id "Unique symbol identifying this loop as a target for recursion"]]}
  Compiler$LetExpr
  (analysis->map
    [expr {:keys [context loop-id] :as env} opt]
    (let [top-env env
          loop? (.isLoop expr)]
      (loop [bindings (.bindingInits expr)
             env (u/ctx env :ctx/expr)
             binds []]
        (if-let [[binit & bindings] (seq bindings)]
          (let [bind-expr (assoc (analysis->map binit env opt)
                                 :local (if loop? :loop :let))
                name (:name bind-expr)]
            (assert (symbol? name) (str "letexpr" bind-expr))
            ;(prn "name" name (keys bind-expr) (:op bind-expr))
            (recur bindings
                   (assoc-in env [:locals name] (u/dissoc-env bind-expr))
                   (conj binds bind-expr)))
          (let [body-env (assoc env :context (if loop? :ctx/return context))
                body (assoc (analysis->map (.body expr)
                                           (merge body-env
                                                  (when loop?
                                                    {:loop-id     loop-id
                                                     :loop-locals (count binds)}))
                                           opt)
                            :body? true)]
            {:op (if loop? :loop :let)
             :form (list (if loop? 'loop* 'let*) 'TODO)
             :env (inherit-env body top-env)
             :bindings binds
             :body body
             :tag (:tag body)
             :o-tag (:o-tag body)
             :children [:bindings :body]})))))

  ;{:op   :local
  ; :doc  "Node for a local symbol"
  ; :keys [[:form "The local symbol"]
  ;        [:assignable? "`true` if the corresponding :binding AST node is :local :field and is declared either ^:volatile-mutable or ^:unsynchronized-mutable"]
  ;        [:name "The uniquified local symbol"]
  ;        [:local "One of :arg, :catch, :fn, :let, :letfn, :loop, :field or :this"]
  ;        ^:optional
  ;        [:arg-id "When :local is :arg, the parameter index"]
  ;        ^:optional
  ;        [:variadic? "When :local is :arg, a boolean indicating whether this parameter binds to a variable number of arguments"]
  ;        [:atom "An atom shared by this :local node, the :binding node this local refers to and all the other :local nodes that refer to this same local"]]}
  Compiler$LocalBinding
  (analysis->map
    [lb env opt]
    (let [init (when-let [init (.init lb)]
                 (analysis->map init env opt))
          form (.sym lb)
          tag (ju/maybe-class (.tag lb))]
      {:op :local
       :name form
       :form form
       :env (inherit-env init env)
       :tag tag
       :o-tag tag
       :atom (atom {})
       :children nil}))

  ;  {:op   :binding
  ;   :doc  "Node for a binding symbol"
  ;   :keys [[:form "The binding symbol"]
  ;          [:name "The uniquified binding symbol"]
  ;          [:local "One of :arg, :catch, :fn, :let, :letfn, :loop, :field or :this"]
  ;          ^:optional
  ;          [:arg-id "When :local is :arg, the parameter index"]
  ;          ^:optional
  ;          [:variadic? "When :local is :arg, a boolean indicating whether this parameter binds to a variable number of arguments"]
  ;          ^:optional ^:children
  ;          [:init "When :local is :let, :letfn or :loop, an AST node representing the bound value"]
  ;          [:atom "An atom shared by this :binding node and all the :local nodes that refer to this binding"]]}
  Compiler$BindingInit
  (analysis->map
    [bi env opt]
    (let [;; Compiler$LocalBinding
          local-binding (analysis->map (.binding bi) env opt)
          ;_ (prn "binding init local-binding" (keys local-binding)
          ;       (:op local-binding))
          init (analysis->map (.init bi) env opt)
          name (:name local-binding)]
      (assert (symbol? name) "bindinginit")
      {:op :binding
       :form name
       :name name
       :env (inherit-env init env)
       :tag (:tag init)
       :o-tag (:o-tag init)
       :local :unknown
       :init init
       :children [:init]}))

  ;; letfn
  Compiler$LetFnExpr
  ; {:op   :letfn
  ;  :doc  "Node for a letfn* special-form expression"
  ;  :keys  [[:form "`(letfn* [binding*] body*)`"]
  ;          ^:children
  ;          [:bindings "A vector of :binding AST nodes with :local :letfn"]
  ;          ^:children
  ;          [:body "Synthetic :do node (with :body? `true`) representing the body of the letfn expression"]]}
  (analysis->map
    [expr env opt]
    (let [binding-inits (mapv #(-> (analysis->map % env opt)
                                   (assoc :local :letfn
                                          :op :binding))
                              (.bindingInits expr))
          benv (update-in env [:locals] merge
                          (into {}
                                (map 
                                  (fn [{:keys [name] :as b}]
                                    [name
                                     {:op :binding :env env
                                      :name name
                                      :form name
                                      :local :letfn}])
                                  binding-inits)))
          binding-inits (mapv #(assoc % :env benv) binding-inits)
          body (-> (analysis->map (.body expr) 
                                  (update-in env [:locals] merge 
                                             (zipmap (map :name binding-inits) binding-inits))
                                  opt)
                   (assoc :body? true))]
      {:op :letfn
       :form (list 'letfn*
                   (mapv (fn [b]
                           (list* (:name b) (map emit-form/emit-form (:methods (:init b)))))
                         binding-inits)
                   (emit-form/emit-form body))
       :env (inherit-env body env)
       :bindings binding-inits
       :body body
       :tag (:tag body)
       :o-tag (:o-tag body)
       :children [:bindings :body]}))

  ;; LocalBindingExpr
  Compiler$LocalBindingExpr
  (analysis->map
    [expr env opt]
    {:post [%]}
    (let [b (analysis->map (.b expr) env opt)
          form (:name b)]
      (assert (symbol? form))
      (assert (contains? (:locals env) form)
              (str form))
      ;(prn "LocalBindingExpr" env)
      (assoc (dissoc ((:locals env) form)
                     :init)
             :op :local
             ;; form has new metadata
             :env env
             :children [])))

  ;; Methods
  ; {:op   :static-call
  ;  :doc  "Node for a static method call"
  ;  :keys [[:form "`(Class/method arg*)`"]
  ;         [:class "The Class the static method belongs to"]
  ;         [:method "The symbol name of the static method"]
  ;         ^:children
  ;         [:args "A vector of AST nodes representing the args to the method call"]
  ;         ^:optional
  ;         [:validated? "`true` if the static method could be resolved at compile time"]]}
  Compiler$StaticMethodExpr
  (analysis->map
    [expr env opt]
    (let [args (mapv #(analysis->map % env opt) (field Compiler$StaticMethodExpr args expr))
          method (when-let [method (field Compiler$StaticMethodExpr method expr)]
                   (@#'reflect/method->map method))
          method-name (symbol (field Compiler$StaticMethodExpr methodName expr))
          tag (ju/maybe-class (field Compiler$StaticMethodExpr tag expr))
          c (field Compiler$StaticMethodExpr c expr)]
      (merge
        {:op :static-call
         :env (env-location env expr)
         :form (list '. c (list* method-name (map emit-form/emit-form args)))
         :class c
         :method method-name
         :args args
         :tag tag
         :o-tag tag
         :children [:args]}
        (when method
          {:validated? true}))))

  Compiler$InstanceMethodExpr
  ;  {:op   :instance-call
  ;   :doc  "Node for an instance method call"
  ;   :keys [[:form "`(.method instance arg*)`"]
  ;          [:method "Symbol naming the invoked method"]
  ;          ^:children
  ;          [:instance "An AST node representing the instance to call the method on"]
  ;          ^:children
  ;          [:args "A vector of AST nodes representing the args passed to the method call"]
  ;          ^:optional
  ;          [:validated? "`true` if the method call could be resolved at compile time"]
  ;          ^:optional
  ;          [:class "If :validated? the class or interface the method belongs to"]]}
  (analysis->map
    [expr env opt]
    (let [target (analysis->map (field Compiler$InstanceMethodExpr target expr) env opt)
          args (mapv #(analysis->map % env opt) (field Compiler$InstanceMethodExpr args expr))
          method-name (symbol (field Compiler$InstanceMethodExpr methodName expr))
          method (when-let [method (field Compiler$InstanceMethodExpr method expr)]
                   (@#'reflect/method->map method))
          tag (ju/maybe-class (field Compiler$InstanceMethodExpr tag expr))]
      (merge
        {:op :instance-call
         :form (list '. (emit-form/emit-form target)
                     (list* method-name (map emit-form/emit-form args)))
         :env (env-location env expr)
         :instance target
         :method method-name
         :args args
         :tag tag
         :o-tag tag
         :children [:instance :args]}
        (when method
          {:validated? true}))))

  ;; Fields
  ;{:op   :static-field
  ; :doc  "Node for a static field access"
  ; :keys [[:form "`Class/field`"]
  ;        [:class "The Class the static field belongs to"]
  ;        [:field "The symbol name of the static field"]
  ;        ^:optional
  ;        [:assignable? "`true` if the static field is set!able"]]}
  Compiler$StaticFieldExpr
  (analysis->map
    [expr env opt]
    (let [tag (ju/maybe-class (field Compiler$StaticFieldExpr tag expr))
          c (field Compiler$StaticFieldExpr c expr)
          fstr (field Compiler$StaticFieldExpr fieldName expr)]
      {:op :static-field
       :form (list '. (emit-form/class->sym c)
                   (symbol (str "-" fstr)))
       :env (env-location env expr)
       :class c
       :field (symbol fstr)
       ;:field (when-let [field (field Compiler$StaticFieldExpr field expr)]
       ;         (@#'reflect/field->map field))
       :tag tag
       :o-tag tag}))

  Compiler$InstanceFieldExpr
  ; {:op   :instance-field
  ;  :doc  "Node for an instance field access"
  ;  :keys  [[:form "`(.-field instance)`"]
  ;          [:field "Symbol naming the field to access"]
  ;          ^:children
  ;          [:instance "An AST node representing the instance to lookup the symbol on"]
  ;          [:assignable? "`true` if the field is set!able"]
  ;          [:class "The class the field belongs to"]]}
  (analysis->map
    [expr env opt]
    (let [target (analysis->map (field Compiler$InstanceFieldExpr target expr) env opt)
          fstr (field Compiler$InstanceFieldExpr fieldName expr)
          tag (ju/maybe-class (field Compiler$InstanceFieldExpr tag expr))]
      {:op :instance-field
       :form (list (symbol (str ".-" fstr)) (emit-form/emit-form target))
       :env (env-location env expr)
       :instance target
       :class (field Compiler$InstanceFieldExpr targetClass expr)
       ;:field (when-let [field (field Compiler$InstanceFieldExpr field expr)]
       ;         (@#'reflect/field->map field))
       :field (symbol fstr)
       :tag tag
       :o-tag tag
       :children [:instance]}))

  ; {:op   :new
  ;  :doc  "Node for a new special-form expression"
  ;  :keys [[:form "`(new Class arg*)`"]
  ;         ^:children
  ;         [:class "A :const AST node with :type :class representing the Class to instantiate"]
  ;         ^:children
  ;         [:args "A vector of AST nodes representing the arguments passed to the Class constructor"]
  ;         ^:optional
  ;         [:validated? "`true` if the constructor call could be resolved at compile time"]]}
  Compiler$NewExpr
  (analysis->map
    [expr env opt]
    (let [args (mapv #(analysis->map % env opt) (.args expr))
          c (.c expr)
          cls {:op :const
               :env env
               :type :class
               :literal? true
               :form (emit-form/class->sym c)
               :val c
               :tag Class
               :o-tag Class}
          ctor (when-let [ctor (.ctor expr)]
                 (@#'reflect/constructor->map ctor))]
      (merge
        {:op :new
         :form (list* 'new (emit-form/emit-form cls) (map emit-form/emit-form args))
         :env 
         ; borrow line numbers from arguments
         (if-let [iexpr (first (filter :line (map :env args)))]
           (inherit-env iexpr env)
           env)
         :class cls
         :args args
         :tag c
         :o-tag c
         :children [:class :args]}
        (when ctor
          {:validated? true}))))

  ;; set literal
  ; {:op   :set
  ;  :doc  "Node for a set literal with attached metadata and/or non literal elements"
  ;  :keys [[:form "`#{item*}`"]
  ;         ^:children
  ;         [:items "A vector of AST nodes representing the items of the set"]]}
  Compiler$SetExpr
  (analysis->map
    [expr env opt]
    (let [keys (mapv #(analysis->map % env opt) (.keys expr))]
      {:op :set
       :env env
       :form `#{~@(map emit-form/emit-form keys)}
       :items keys
       :tag clojure.lang.IPersistentSet
       :o-tag clojure.lang.IPersistentSet
       :children [:items]}))

  ;; vector literal
  ; {:op   :vector
  ;  :doc  "Node for a vector literal with attached metadata and/or non literal elements"
  ;  :keys [[:form "`[item*]`"]
  ;         ^:children
  ;         [:items "A vector of AST nodes representing the items of the vector"]]}
  Compiler$VectorExpr
  (analysis->map
    [expr env opt]
    (let [args (mapv #(analysis->map % env opt) (.args expr))]
      {:op :vector
       :env env
       :form `[~@(map emit-form/emit-form args)]
       :items args
       :tag clojure.lang.IPersistentVector
       :o-tag clojure.lang.IPersistentVector
       :children [:items]}))

  ;; map literal
  ; {:op   :map
  ;  :doc  "Node for a map literal with attached metadata and/or non literal elements"
  ;  :keys [[:form "`{[key val]*}`"]
  ;         ^:children
  ;         [:keys "A vector of AST nodes representing the keys of the map"]
  ;         ^:children
  ;         [:vals "A vector of AST nodes representing the vals of the map"]]}
  Compiler$MapExpr
  (analysis->map
    [expr env opt]
    (let [kvs (partition 2 (.keyvals expr))
          ks  (mapv #(analysis->map % env opt) (map first kvs))
          vs  (mapv #(analysis->map % env opt) (map second kvs))]
      {:op :map
       :env env
       ;; FIXME use transducers when dropping 1.6 support
       :form (into {}
                   (map
                     (fn [k v]
                       [(emit-form/emit-form k)
                        (emit-form/emit-form v)])
                     ks
                     vs))
       :keys ks
       :vals vs
       :tag clojure.lang.IPersistentMap
       :o-tag clojure.lang.IPersistentMap
       :children [:keys :vals]}))

  ;; Untyped
  Compiler$MonitorEnterExpr
  (analysis->map
    [expr env opt]
    (let [target (analysis->map (field Compiler$MonitorEnterExpr target expr) env opt)]
      {:op :monitor-enter
       :env env
       :form (list 'monitor-enter (emit-form/emit-form target))
       :target target
       :tag nil
       :o-tag nil
       :children [:target]}))

  Compiler$MonitorExitExpr
  ;{:op   :monitor-exit
  ; :doc  "Node for a monitor-exit special-form statement"
  ; :keys [[:form "`(monitor-exit target)`"]
  ;        ^:children
  ;        [:target "An AST node representing the monitor-exit sentinel"]]}
  (analysis->map
    [expr env opt]
    (let [target (analysis->map (field Compiler$MonitorExitExpr target expr) env opt)]
      (merge
        {:op :monitor-exit
         :env env
         :form (list 'monitor-exit (emit-form/emit-form target))
         :target target
         :tag nil
         :o-tag nil
         :children [:target]})))

  Compiler$ThrowExpr
  ; {:op   :throw
  ;  :doc  "Node for a throw special-form statement"
  ;  :keys [[:form "`(throw exception)`"]
  ;         ^:children
  ;         [:exception "An AST node representing the exception to throw"]]}
  (analysis->map
    [expr env opt]
    (let [exception (analysis->map (field Compiler$ThrowExpr excExpr expr) env opt)]
      {:op :throw
       :form (list 'throw (emit-form/emit-form exception))
       :env env
       :exception exception
       :tag nil
       :o-tag nil
       :children [:exception]}))

  ;; Invokes
  ; {:op   :invoke
  ;  :doc  "Node for an invoke expression"
  ;  :keys [[:form "`(f arg*)`"]
  ;         ^:children
  ;         [:fn "An AST node representing the function to invoke"]
  ;         ^:children
  ;         [:args "A vector of AST nodes representing the args to the function"]
  ;         ^:optional
  ;         [:meta "Map of metadata attached to the invoke :form"]]}
  Compiler$InvokeExpr
  (analysis->map
    [expr env opt]
    (let [fexpr (analysis->map (field Compiler$InvokeExpr fexpr expr) env opt)
          args (mapv #(analysis->map % env opt) (field Compiler$InvokeExpr args expr))
          env (env-location env expr)
          tag (ju/maybe-class (field Compiler$InvokeExpr tag expr))
          form (list* (emit-form/emit-form fexpr) (map emit-form/emit-form args))]
      (cond
        ;; TAJ always compiles :keyword-invoke sites where possible, Compiler.java
        ;; only compiles when inside a function body. We follow Compiler.java, there's
        ;; not much difference anyway, except for the TEJ output.
        #_(and (== 1 (count args))
             (keyword? (:val fexpr)))
        #_{:op :keyword-invoke
         :form form
         :env env
         :keyword fexpr
         :tag tag
         :o-tag tag
         :target (first args)
         :children [:keyword :target]}

        :else
        {:op :invoke
         :form form
         :env env
         :fn fexpr
         :tag tag
         :o-tag tag
         :args args
         :children [:fn :args]})))

         ;:is-protocol (field Compiler$InvokeExpr isProtocol expr)
         ;:is-direct (field Compiler$InvokeExpr isDirect expr)
         ;:site-index (field Compiler$InvokeExpr siteIndex expr)
         ;:protocol-on (field Compiler$InvokeExpr protocolOn expr)
        ;(when-let [m (field Compiler$InvokeExpr onMethod expr)]
        ;  {:method (@#'reflect/method->map m)})

  ; {:op   :keyword-invoke
  ;  :doc  "Node for an invoke expression where the fn is a not-namespaced keyword and thus a keyword callsite can be emitted"
  ;  :keys [[:form "`(:k instance)`"]
  ;         ^:children
  ;         [:keyword "An AST node representing the keyword to lookup in the instance"]
  ;         ^:children
  ;         [:target "An AST node representing the instance to lookup the keyword in"]]}
  Compiler$KeywordInvokeExpr
  (analysis->map
    [expr env opt]
    (let [target (analysis->map (field Compiler$KeywordInvokeExpr target expr) env opt)
          kw (analysis->map (field Compiler$KeywordInvokeExpr kw expr) env opt)
          tag (ju/maybe-class (field Compiler$KeywordInvokeExpr tag expr))
          form (list (emit-form/emit-form kw) (emit-form/emit-form target))]
      {:op :keyword-invoke
       :form form
       :env (env-location env expr)
       :keyword kw
       :tag tag
       :o-tag tag
       :target target
       :children [:keyword :target]}))

  ;; TheVarExpr
  ; {:op   :the-var
  ;  :doc  "Node for a var special-form expression"
  ;  :keys [[:form "`(var var-name)`"]
  ;         [:var "The Var object this expression refers to"]]}
  Compiler$TheVarExpr
  (analysis->map
    [expr env opt]
    (let [^clojure.lang.Var var (.var expr)]
      {:op :the-var
       :tag clojure.lang.Var
       :o-tag clojure.lang.Var
       :form (list 'var (symbol (str (ns-name (.ns var))) (str (.sym var))))
       :env env
       :var var}))

  ;; VarExpr
  ; {:op   :var
  ;  :doc  "Node for a var symbol"
  ;  :keys [[:form "A symbol naming the var"]
  ;         [:var "The Var object this symbol refers to"]
  ;         ^:optional
  ;         [:assignable? "`true` if the Var is :dynamic"]]}
  Compiler$VarExpr
  (analysis->map
    [expr env opt]
    (let [^clojure.lang.Var var (.var expr)
          meta (meta var)
          tag (ju/maybe-class (.tag expr))]
      {:op :var
       :env env
       :var var
       :meta meta
       :tag tag
       :o-tag tag
       :assignable? (boolean (:dynamic meta))
       :arglists (:arglists meta)
       :form (symbol (str (ns-name (.ns var)))
                     (str (.sym var)))}))

  ;; UnresolvedVarExpr
  Compiler$UnresolvedVarExpr
  (analysis->map
    [expr env opt]
    (assert nil "UnresolvedVarExpr")
    (let []
      (merge
        {:op :unresolved-var
         :env env
         :sym (.symbol expr)}
        (when (:java-obj opt)
          {:Expr-obj expr}))))

  ;; ObjExprs
  Compiler$ObjExpr
  (analysis->map
    [expr env opt]
    (assert nil "ObjExprs")
    (merge
      {:op :obj-expr
       :env env
       :tag (.tag expr)}
      (when (:java-obj opt)
        {:Expr-obj expr})))

  ;; FnExpr (extends ObjExpr)
  ; {:op   :method
  ;  :doc  "Node for a method in a deftype* or reify* special-form expression"
  ;  :keys [[:form "`(method [this arg*] body*)`"]
  ;         [:bridges "A list of signature for bridge methods to emit"]
  ;         [:interface "The interface (or Object) this method belongs to"]
  ;         ^:children
  ;         [:this "A :binding AST node with :local :this representing the \"this\" local"]
  ;         [:loop-id "Unique symbol identifying this method as a target for recursion"]
  ;         [:name "The symbol name of this method"]
  ;         ^:children
  ;         [:params "A vector of AST :binding nodes with :local :arg representing the arguments of the method"]
  ;         [:fixed-arity "The number of args this method takes"]
  ;         ^:children
  ;         [:body "Synthetic :do node (with :body? `true`) representing the body of this method"]]}
  Compiler$NewInstanceMethod
  (analysis->map
    [obm env opt]
    (let [loop-id (gensym "loop_")
          this (-> (analysis->map ((field Compiler$ObjMethod indexlocals obm) 0) env opt)
                   (assoc :local :this
                          :op :binding)
                   (dissoc :env))
          penv (update-in env [:locals] assoc (:name this) this)
          ;; FIXME use transients
          params (vec
                   (map-indexed 
                     #(-> (analysis->map %2 penv opt)
                          (assoc :local :arg
                                 :op :binding
                                 :arg-id %1
                                 :variadic? false)
                          (dissoc :children :init))
                     (field Compiler$ObjMethod argLocals obm)))
          menv (-> penv
                   (update-in [:locals] merge (zipmap (map :name params) params))
                   (assoc :loop-locals (count params)))
          body (assoc (analysis->map (.body obm) (assoc menv :loop-id loop-id) opt)
                      :body? true)
          name (symbol (field Compiler$NewInstanceMethod name obm))]
      {:op :method
       :env (env-location env obm)
       :this (assoc this :op :binding)
       :bridges ()
       :name name
       :loop-id loop-id
       :fixed-arity (count params)
       :params params
       :body body
       :tag (:tag body)
       :o-tag (:o-tag body)
       :children [:this :params :body]}))

  ; {:op   :fn-method
  ;  :doc  "Node for an arity method in a fn* expression"
  ;  :keys [[:form "`([arg*] body*)`"]
  ;         [:loop-id "Unique symbol identifying this method as a target for recursion"]
  ;         [:variadic? "`true` if this fn-method takes a variable number of arguments"]
  ;         ^:children
  ;         [:params "A vector of :binding AST nodes with :local :arg representing this fn-method args"]
  ;         [:fixed-arity "The number of non-variadic args this fn-method takes"]
  ;         ^:children
  ;         [:body "Synthetic :do node (with :body? `true`) representing the body of this fn-method"]]}
  Compiler$FnMethod
  (analysis->map
    [obm env opt]
    (let [loop-id (gensym "loop_")
          rest-param (when-let [rest-param (.restParm obm)]
                       (assoc (analysis->map rest-param env opt)
                              :variadic? true
                              :local :arg
                              :op :binding))
          required-params (mapv #(assoc (analysis->map %1 env opt)
                                        :variadic? false
                                        :local :arg
                                        :arg-id %2
                                        :op :binding)
                                (.reqParms obm)
                                (range))
          params-expr (into required-params
                            (when rest-param
                              [rest-param]))
          ;_ (prn "params-expr" (map :op params-expr))
          body-env (into (update-in env [:locals]
                                    merge (zipmap (map :name params-expr) (map u/dissoc-env params-expr)))
                         {:context     :ctx/return
                          :loop-id     loop-id
                          :loop-locals (count params-expr)})
          body (analysis->map (.body obm) body-env opt)]
      (merge
        {:op :fn-method
         :loop-id loop-id
         :variadic? (boolean rest-param)
         :params params-expr
         :fixed-arity (count required-params)
         :body (assoc body :body? true)
         :env env
         :tag (:tag body)
         :o-tag (:o-tag body)
         ;; Map LocalExpr@xx -> LocalExpr@xx
         ;;:locals (map analysis->map (keys (.locals obm)) (repeat env) (repeat opt))
         :children [:params :body]})))

  ; {:op   :fn
  ;  :doc  "Node for a fn* special-form expression"
  ;  :keys [[:form "`(fn* name? [arg*] body*)` or `(fn* name? method*)`"]
  ;         [:variadic? "`true` if this function contains a variadic arity method"]
  ;         [:max-fixed-arity "The number of arguments taken by the fixed-arity method taking the most arguments"]
  ;         ^:optional ^:children
  ;         [:local "A :binding AST node with :local :fn representing the function's local name, if one is supplied"]
  ;         ^:children
  ;         [:methods "A vector of :fn-method AST nodes representing the fn method arities"]
  ;         [:once "`true` if the fn is marked as `^:once fn*`, meaning it will only be executed once and thus allowing for the clearing of closed-over locals"]
  Compiler$FnExpr
  (analysis->map
    [expr env opt]
    (let [once (field-accessor Compiler$ObjExpr 'onceOnly expr)
          this-name (when-let [nme (.thisName expr)]
                      (symbol nme))
          this (when this-name
                 {:op :binding
                  :env env
                  :form this-name
                  :name this-name
                  :local :fn})
          menv (assoc env :once once)
          menv (if this
                 (update-in menv [:locals] assoc (:name this) this)
                 menv)
          variadic-method (when-let [variadic-method (.variadicMethod expr)]
                            (analysis->map variadic-method menv opt))
          methods-no-variadic (mapv #(analysis->map % menv opt) (.methods expr))
          methods (into methods-no-variadic
                        (when variadic-method
                          [variadic-method]))
          fixed-arities (seq (map :fixed-arity methods-no-variadic))
          max-fixed-arity (when fixed-arities (apply max fixed-arities))
          tag (ju/maybe-class(.tag expr))]
      (merge
        {:op :fn
         :env (env-location env expr)
         :form (list* 'fn* 
                      (concat
                        (when this
                          [(emit-form/emit-form this)])
                        (map emit-form/emit-form methods)))
         :methods methods
         :variadic? (boolean variadic-method)
         :tag   tag
         :o-tag tag
         :max-fixed-arity max-fixed-arity
         :once once
         :children (vec
                     (concat (when this-name
                               [:local])
                             [:methods]))}
        (when this-name
          ;; FIXME what is a :binding?
          {:local this}))))

  ;; NewInstanceExpr
  ; {:op   :deftype
  ;  :doc  "Node for a deftype* special-form expression"
  ;  :keys [[:form "`(deftype* name class.name [arg*] :implements [interface*] method*)`"]
  ;         [:interfaces "A set of the interfaces implemented by the type"]
  ;         [:name "The symbol name of the deftype"]
  ;         [:class-name "A class for the deftype, should *never* be instantiated or used on instance? checks as this will not be the same class the deftype will evaluate to after compilation"]
  ;         ^:children
  ;         [:fields "A vector of :binding AST nodes with :local :field representing the deftype fields"]
  ;         ^:children
  ;         [:methods "A vector :method AST nodes representing the deftype methods"]]}
;FIXME find vector of interfaces this implements (I think it's in mmap + IType)
  Compiler$NewInstanceExpr
  (analysis->map
    [expr env opt]
    ;(prn "NewInstanceExpr")
    (let [methods (mapv #(analysis->map % env opt) (field Compiler$NewInstanceExpr methods expr))
          ;_ (prn "fields")
          ;; don't know what a MethodParamExpr is, just use the key
          fields (mapv (fn [kv]
                         (let [name (first kv)]
                           ;(analysis->map (val kv) env opt)
                           {:op :binding
                            :env env
                            :name name
                            :form name
                            :local :field
                            :mutable (when (#{:unsynchronized-mutable
                                              :volatile-mutable}
                                             (meta name))
                                       true)}))
                       (field Compiler$ObjExpr fields expr))
          ;_ (prn "before name")
          name (symbol (str (:ns env)) (peek (string/split (.name expr) #"\.")))
          ;_ (prn "after name")
          class-name (.compiledClass expr) ;or  #_(.internalName expr) ?
          interfaces (remove
                       #{Object}
                       (concat
                         (map (fn [^java.lang.reflect.Method m]
                                (.getDeclaringClass m))
                              (vals (field Compiler$NewInstanceExpr mmap expr)))
                         [clojure.lang.IType]))
          tag (ju/maybe-class (.tag expr))]
      ;(prn :compiled-class (.compiledClass expr))
      ;(prn :internal-name (.internalName expr))
      ;(prn :this-name (.thisName expr))
      ;(prn "name" name)
      ;(prn "mmap" (field Compiler$NewInstanceExpr mmap expr))
      {:op :deftype
       :form (list* 'deftype* name class-name
                    [] ;; TODO
                    :implements (mapv emit-form/class->sym interfaces)
                    (map emit-form/emit-form methods))
       :name name
       :env (env-location env expr)
       :methods methods
       :fields fields
       :class-name class-name

       :interfaces (set interfaces)

       ;:mmap (field Compiler$NewInstanceExpr mmap expr)
       ;:compiled-class (.compiledClass expr)
       ;:internal-name (.internalName expr)
       ;:this-name (.thisName expr)

       ;(Vec Symbol)
       ;:hinted-fields (field Compiler$ObjExpr hintedFields expr)
       ;:covariants (field Compiler$NewInstanceExpr covariants expr)
       :tag tag
       :o-tag tag
       :children [:fields :methods]}))

  ;; InstanceOfExpr
  ; {:op   :instance?
  ;  :doc  "Node for a clojure.core/instance? call where the Class is known at compile time"
  ;  :keys [[:form "`(clojure.core/instance? Class x)`"]
  ;         [:class "The Class to test the :target for instanceability"]
  ;         ^:children
  ;         [:target "An AST node representing the object to test for instanceability"]]}
  Compiler$InstanceOfExpr
  (analysis->map
    [expr env opt]
    (let [exp (analysis->map (field Compiler$InstanceOfExpr expr expr) env opt)
          ^Class cls (field Compiler$InstanceOfExpr c expr)]
      {:op :instance?
       :env env
       :class cls
       :target exp
       :tag Boolean/TYPE
       :o-tag Boolean/TYPE
       :form (list 'instance? (symbol (.getName cls)) (emit-form/emit-form exp))
       :children [:target]}))

  ;; MetaExpr
  ; {:op   :with-meta
  ;  :doc  "Node for a non quoted collection literal or fn/reify expression with attached metadata"
  ;  :keys [[:form "Non quoted collection literal or fn/reify expression with attached metadata"]
  ;         ^:children
  ;         [:meta "An AST node representing the metadata of expression. The node will be either a :map node or a :const node with :type :map"]
  ;         ^:children
  ;         [:expr "The expression this metadata is attached to, :op is one of :vector, :map, :set, :fn or :reify"]]}]}
  Compiler$MetaExpr
  (analysis->map
    [expr env opt]
    (let [meta (analysis->map (.meta expr) env opt)
          meta (if (#{:quote} (:op meta))
                 (:expr meta)
                 meta)
          _ (assert (#{:const :map} (:op meta))
                    (str "MetaExpr :meta must be a :const or :map node"))
          the-expr (analysis->map (.expr expr) env opt)]
      {:op :with-meta
       :env env
       :form (emit-form/emit-form the-expr) ;FIXME add meta
       :meta meta
       :expr the-expr
       :tag (:tag the-expr)
       :o-tag (:o-tag the-expr)
       :children [:meta :children]}))

  ;; do
  ; {:op   :do
  ;  :doc  "Node for a do special-form expression or for another special-form's body"
  ;  :keys [[:form "`(do statement* ret)`"]
  ;         ^:children
  ;         [:statements "A vector of AST nodes representing all but the last expression in the do body"]
  ;         ^:children
  ;         [:ret "An AST node representing the last expression in the do body (the block's return value)"]
  ;         ^:optional
  ;         [:body? "`true` if this node is a synthetic body"]]}
  Compiler$BodyExpr
  (analysis->map
    [expr env opt]
    (let [[statements ret] (loop [statements [] [e & exprs] (.exprs expr)]
                             (if exprs
                               (recur (conj statements (analysis->map e env opt)) exprs)
                               [statements (analysis->map e env opt)]))]
      {:op :do
       :env (inherit-env ret env)
       :form (list* 'do (map emit-form/emit-form (concat statements [ret])))
       :statements statements
       :ret ret
       :tag (:tag ret)
       :o-tag (:o-tag ret)
       :children [:statements :ret]}))

  ;; if
  ; {:op   :if
  ;  :doc  "Node for an if special-form expression"
  ;  :keys [[:form "`(if test then else?)`"]
  ;         ^:children
  ;         [:test "An AST node representing the test expression"]
  ;         ^:children
  ;         [:then "An AST node representing the expression's return value if :test evaluated to a truthy value"]
  ;         ^:children
  ;         [:else "An AST node representing the expression's return value if :test evaluated to a falsey value, if not supplied it will default to a :const node representing nil"]]}
  Compiler$IfExpr
  (analysis->map
    [expr env opt]
    (let [test (analysis->map (.testExpr expr) env opt)
          then (analysis->map (.thenExpr expr) env opt)
          else (analysis->map (.elseExpr expr) env opt)
          tag (when (.hasJavaClass expr)
                (.getJavaClass expr))]
      {:op :if
       :env (env-location env expr)
       :form (list* 'if (map emit-form/emit-form [test then else]))
       :test test
       :then then
       :else else
       :tag tag
       :o-tag tag
       :children [:test :then :else]}))

  ;; case
  ;; (from Compiler.java)
  ;;  //(case* expr shift mask default map<minhash, [test then]> table-type test-type skip-check?)
  ; {:op   :case
  ;  :doc  "Node for a case* special-form expression"
  ;  :keys [[:form "`(case* expr shift maks default case-map switch-type test-type skip-check?)`"]
  ;         ^:children
  ;         [:test "The AST node for the expression to test against"]
  ;         ^:children
  ;         [:tests "A vector of :case-test AST nodes, each node has a corresponding :case-then node in the :thens field"]
  ;         ^:children
  ;         [:thens "A vector of :case-then AST nodes, each node has a corresponding :case-test node in the :tests field"]
  ;         ^:children
  ;         [:default "An AST node representing the default value of the case expression"]
  ;         [:shift]
  ;         [:mask]
  ;         [:low]
  ;         [:high]
  ;         [:switch-type "One of :sparse or :compact"]
  ;         [:test-type "One of :int, :hash-equiv or :hash-identity"]
  ;         [:skip-check? "A set of case ints for which equivalence checking should not be done"]]}
  Compiler$CaseExpr
  (analysis->map
    [expr env opt]
    (let [the-expr (analysis->map (.expr expr) env opt)
          tests-map (.tests expr)
          thens-map (.thens expr)
          [low high] ((juxt first last) (keys tests-map)) ;;tests-map is a sorted-map
          [tests thens] (reduce (fn [[tests thens] [h tst]]
                                  (let [test-expr (analysis->map tst env opt)
                                        then-expr (analysis->map (get thens-map h) env opt)]
                                    [(conj tests {:op       :case-test
                                                  :form     (emit-form/emit-form test-expr)
                                                  :env      env
                                                  :hash     h
                                                  :test     test-expr
                                                  :children [:test]})
                                     (conj thens {:op       :case-then
                                                  :form     (emit-form/emit-form then-expr)
                                                  :env      env
                                                  :hash     h
                                                  :then     then-expr
                                                  :children [:then]})]))
                                [[] []] tests-map)
          default (analysis->map (.defaultExpr expr) env opt)
          tag (when (.hasJavaClass expr)
                (.getJavaClass expr))]
      {:op :case
       :env (env-location env expr)
       :test (assoc the-expr :case-test true)
       :tests tests
       :thens thens
       :default default
       :shift (.shift expr)
       :mask (.mask expr)
       :low low
       :high high
       :switch-type (.switchType expr)
       :test-type (.testType expr)
       :skip-check? (.skipCheck expr)
       :tag tag
       :o-tag tag
       :children [:test :tests :thens :default]}))


  ;; ImportExpr
  ; {:op   :import
  ;  :doc  "Node for a clojure.core/import* special-form expression"
  ;  :keys [[:form "`(clojure.core/import* \"qualified.class\")`"]
  ;         [:class "String representing the qualified class to import"]]}
  Compiler$ImportExpr
  (analysis->map
    [expr env opt]
    (let [c (.c expr)]
      (assert (string? c))
      {:op :import
       :env env
       :form (list 'clojure.core/import* c)
       :class c
       :tag nil
       :o-tag nil
       ; :validated? true ?
       }))

  ;; AssignExpr (set!)
  ; {:op   :set!
  ;  :doc  "Node for a set! special-form expression"
  ;  :keys [[:form "`(set! target val)`"]
  ;         ^:children
  ;         [:target "An AST node representing the target of the set! expression, must be :assignable?"]
  ;         [:val "An AST node representing the new value for the target"]]}
  Compiler$AssignExpr
  (analysis->map
    [expr env opt]
    (let [target (analysis->map (.target expr) env opt)
          val (analysis->map (.val expr) env opt)
          tag (when (.hasJavaClass expr)
                (.getJavaClass expr))]
      {:op :set!
       :form (list 'set! 
                   (emit-form/emit-form target)
                   (emit-form/emit-form val))
       :env env
       :target target
       :val val
       :tag tag
       :o-tag tag
       :children [:target :val]}))

  ;;TryExpr
  Compiler$TryExpr$CatchClause
  ;{:op   :catch
  ; :doc  "Node for a catch expression"
  ; :keys [[:form "`(catch class local body*)`"]
  ;        ^:children
  ;        [:class "A :const AST node with :type :class representing the type of exception to catch"]
  ;        ^:children
  ;        [:local "The :binding AST node for the caught exception"]
  ;        ^:children
  ;        [:body "Synthetic :do AST node (with :body? `true`)  representing the body of the catch clause"]]}
  (analysis->map
    [ctch env opt]
    (let [local-binding (-> (analysis->map (.lb ctch) env opt)
                            (assoc 
                              :local :catch
                              :op :binding)
                            (dissoc :children))
          c (.c ctch)
          cls {:op :const
               :env env
               :type :class
               :literal? true
               :form (emit-form/class->sym c)
               :val c
               :tag Class
               :o-tag Class}
          handler (assoc (analysis->map (.handler ctch) 
                                        (-> env
                                            (update-in [:locals]
                                                       assoc (:name local-binding)
                                                       local-binding)
                                            (assoc :no-recur true))
                                        opt)
                         :body? true)]
      {:op :catch
       :form (list 'catch 
                   (emit-form/emit-form cls)
                   (emit-form/emit-form local-binding)
                   (emit-form/emit-form handler))
       :env env
       :class cls
       :local local-binding
       :body handler
       :children [:class :local :body]}))

  ;{:op   :try
  ; :doc  "Node for a try special-form expression"
  ; :keys  [[:form "`(try body* catch* finally?)`"]
  ;         ^:children
  ;         [:body "Synthetic :do AST node (with :body? `true`) representing the body of this try expression"]
  ;         ^:children
  ;         [:catches "A vector of :catch AST nodes representing the catch clauses of this try expression"]
  ;         ^:optional ^:children
  ;         [:finally "Synthetic :do AST node (with :body? `true`) representing the final clause of this try expression"]]}
  Compiler$TryExpr
  (analysis->map
    [expr env opt]
    (let [try-expr (-> (analysis->map (.tryExpr expr) (assoc env :in-try true) opt)
                       (assoc :body? true))
          catch-exprs (mapv #(analysis->map % env opt) (.catchExprs expr))
          finally-expr (when-let [finally-expr (.finallyExpr expr)]
                         (assoc (analysis->map finally-expr env opt)
                                :body? true))
          tag (when (.hasJavaClass expr)
                (.getJavaClass expr))]
      {:op :try
       :form (list* 'try 
                    (emit-form/emit-form try-expr)
                    (concat (map emit-form/emit-form catch-exprs)
                            (when finally-expr
                              [(list 'finally (emit-form/emit-form finally-expr))])))
       :env env
       :body try-expr
       :catches catch-exprs
       ;; can be nil like in TA
       :finally finally-expr
       ;:ret-local (.retLocal expr)
       ;:finally-local (.finallyLocal expr)
       :tag tag
       :o-tag tag
       :children (into [:body :catches]
                       (when finally-expr
                         [:finally]))}))

  ;; RecurExpr
  ; {:op   :recur
  ;  :doc  "Node for a recur special-form expression"
  ;  :keys [[:form "`(recur expr*)`"]
  ;         ^:children
  ;         [:exprs "A vector of AST nodes representing the new bound values for the loop binding on the next loop iteration"]
  ;         [:loop-id "Unique symbol identifying the enclosing loop target"]]}
  Compiler$RecurExpr
  (analysis->map
    [expr env opt]
    (let [;loop-locals (map analysis->map (.loopLocals expr) (repeat env) (repeat opt))
          args (mapv #(analysis->map % env opt) (.args expr))
          tag (.getJavaClass expr)]
      {:op :recur
       :form (list* 'recur (map emit-form/emit-form args))
       :env (env-location env expr)
       ;:loop-locals loop-locals
       :loop-id (:loop-id env)
       :exprs args
       :tag tag
       :o-tag tag
       :children [:exprs]}))

;; thrown away by NewInstanceMethod
  Compiler$MethodParamExpr
  (analysis->map
    [expr env opt]
    (let []
      (merge
        {:op :method-param
         :env env
         :class (.getJavaClass expr)
         :can-emit-primitive (.canEmitPrimitive expr)}
        (when (:java-obj opt)
          {:Expr-obj expr})))))

(defmulti keyword->Context identity)
(defmethod keyword->Context :ctx/statement [_] Compiler$C/STATEMENT)
(defmethod keyword->Context :ctx/expr      [_] #_Compiler$C/EXPRESSION
  ;; EXPRESSION doesn't work too well, eg. (analyze-form '(let []))
  Compiler$C/EVAL)
(defmethod keyword->Context :ctx/return    [_] Compiler$C/RETURN)
;; :eval Compiler$C/EVAL

;; requires clojure 1.7
(defn ^:private analyzer-bindings-one [env]
  {Compiler/LOADER (RT/makeClassLoader)
   Compiler/SOURCE_PATH (:file env)
   Compiler/SOURCE (:file env)
   Compiler/METHOD nil
   Compiler/LOCAL_ENV nil
   Compiler/LOOP_LOCALS nil
   Compiler/NEXT_LOCAL_NUM 0
   #'*ns* (:ns env)
   RT/READEVAL true
   Compiler/LINE_BEFORE (:line env)
   Compiler/LINE_AFTER (:line env)
   RT/UNCHECKED_MATH @RT/UNCHECKED_MATH
   #'*warn-on-reflection* *warn-on-reflection*
   Compiler/COLUMN_BEFORE (:column env)
   Compiler/COLUMN_AFTER (:column env)
   RT/DATA_READERS @RT/DATA_READERS})

(defn- analyze*
  "Must be called after binding the appropriate Compiler and RT dynamic Vars."
  ([env form] (analyze* env form {}))
  ([env form opts]
   (let [context (keyword->Context (:context env))
         env (merge env
                    (when-let [file (and (not= *file* "NO_SOURCE_FILE")
                                         *file*)]
                      {:file file}))
         expr-ast (try
                    (with-bindings (analyzer-bindings-one env)
                      (Compiler/analyze context form))
                    (catch RuntimeException e
                      (throw (repl/root-cause e))))]
     (with-bindings (merge {;#'ana/macroexpand-1 macroexpand-1
                            ;#'ana/create-var    create-var
                            ;#'ana/parse         parse
                            ;#'ana/var?          var?
                            ;#'elides            (merge {:fn    #{:line :column :end-line :end-column :file :source}
                            ;                            :reify #{:line :column :end-line :end-column :file :source}}
                            ;                           elides)
                            ;#'*ns*              (the-ns (:ns env))
                            }
                           (:bindings opts))
       (-> (analysis->map expr-ast env opts)
           (assoc :top-level true
                  :eval-fn #(method-accessor (class expr-ast) 'eval expr-ast [])))))))

(defn analyze-one
  "Analyze a single form"
  ([env form] (analyze-one env form {}))
  ([env form opt] (analyze* env form opt)))

(defn forms-seq
  "Lazy seq of forms in a Clojure or ClojureScript file."
  [^java.io.PushbackReader rdr]
  (let [eof (reify)]
    (lazy-seq
      (let [form (read rdr nil eof)]
        (when-not (identical? form eof)
          (lazy-seq (cons form (forms-seq rdr))))))))

(defn ^:private munge-ns [ns-sym]
  (-> (name ns-sym)
      (string/replace "." "/")
      (string/replace "-" "_")
      (str ".clj")))
       
(defn uri-for-ns 
  "Returns a URI representing the namespace. Throws an
  exception if URI not found."
  [ns-sym]
  (let [source-path (munge-ns ns-sym) 
        uri (io/resource source-path)]
    (when-not uri
      (throw (Exception. (str "No file found for namespace " ns-sym))))
    uri))

(defn ^LineNumberingPushbackReader
  pb-reader-for-ns
  "Returns a LineNumberingPushbackReader for namespace ns-sym"
  [ns-sym]
  (let [uri (uri-for-ns ns-sym)]
    (LineNumberingPushbackReader. (io/reader uri))))

(defonce ^:private Compiler-members (set (map :name (:members (reflect/type-reflect RT)))))
(defonce ^:private RT-members (set (map :name (:members (reflect/type-reflect RT)))))

(defmacro ^:private analyzer-bindings [source-path pushback-reader]
  `(merge
     {Compiler/LOADER (RT/makeClassLoader)
      Compiler/SOURCE_PATH (str ~source-path)
      Compiler/SOURCE (str ~source-path)
      Compiler/METHOD nil
      Compiler/LOCAL_ENV nil
      Compiler/LOOP_LOCALS nil
      Compiler/NEXT_LOCAL_NUM 0
      RT/CURRENT_NS @RT/CURRENT_NS
      Compiler/LINE_BEFORE (.getLineNumber ~pushback-reader)
      Compiler/LINE_AFTER (.getLineNumber ~pushback-reader)
      RT/UNCHECKED_MATH @RT/UNCHECKED_MATH}
     ~(when (RT-members 'WARN_ON_REFLECTION)
        `{(field RT ~'WARN_ON_REFLECTION) @(field RT ~'WARN_ON_REFLECTION)})
     ~(when (Compiler-members 'COLUMN_BEFORE)
        `{Compiler/COLUMN_BEFORE (.getColumnNumber ~pushback-reader)})
     ~(when (Compiler-members 'COLUMN_AFTER)
        `{Compiler/COLUMN_AFTER (.getColumnNumber ~pushback-reader)})
     ~(when (RT-members 'DATA_READERS)
        `{RT/DATA_READERS @RT/DATA_READERS})))

(defn analyze-file
  "Takes a file path and optionally a pushback reader.
  Returns a vector of maps representing the ASTs of the forms
  in the target file.

  Options:
  - :reader  a pushback reader to use to read the namespace forms
  - :opt     a map of analyzer options
    - :children
      when true, include a :children key with all child expressions of each node
    - :java-obj
      when true, include a :java-obj key with the node's corresponding Java object

  eg. (analyze-file \"my/ns.clj\")"
  [source-path & {:keys [reader opt] 
                  :or {reader (LineNumberingPushbackReader. (io/reader (io/resource source-path)))}}]
  (let [eof (reify)
        ^LineNumberingPushbackReader 
        pushback-reader (if (instance? LineNumberingPushbackReader reader)
                          reader
                          (LineNumberingPushbackReader. reader))]
    (with-bindings (analyzer-bindings source-path pushback-reader)
      (loop [form (read pushback-reader nil eof)
             out []]
        (if (identical? form eof)
          out
          (let [env {:ns (ns-name *ns*)
                     :source-path source-path
                     :locals {}}
                expr-ast (Compiler/analyze (keyword->Context :eval) form)
                m (analysis->map expr-ast env opt)
                _ (when *eval-ast*
                    (method-accessor Compiler$Expr 'eval expr-ast []))]
            (recur (read pushback-reader nil eof) (conj out m))))))))

(defn analyze-ns
  "Takes a LineNumberingPushbackReader and a namespace symbol.
  Returns a vector of maps, with keys :op, :env. If expressions
  have children, will have :children entry.

  Options:
  - :reader  a pushback reader to use to read the namespace forms
  - :opt     a map of analyzer options
    - :children
      when true, include a :children key with all child expressions of each node
    - :java-obj
      when true, include a :java-obj key with the node's corresponding Java object

  eg. (analyze-ns 'my-ns :opt {:children true} :reader (pb-reader-for-ns 'my.ns))"
  [source-nsym & {:keys [reader opt] :or {reader (pb-reader-for-ns source-nsym)}}]
  (let [source-path (munge-ns source-nsym)]
    (analyze-file source-path :reader reader :opt opt)))


(comment
  (ast 
    (try (throw (Exception.)) 
      (catch Exception e (throw e)) 
      (finally 33)))

  (ast
    (let [b 1] 
      (fn [& a] 1)))

  (ast (Integer. (+ 1 1)))

  (ast (map io/file [1 2]))

  (ast (do 
         (require '[clojure.repl :refer [pst]])
         (pst)))
  (ast (deftype A [a b]
         Object
         (toString [this])))
  
  ;children
  ; - what about optional keys? eg. :local-binding's :init? do we need an :optional case, or
  ;   maybe a `:when child-expr` will be sufficient?
  (->
    (let [expr (ast (let [a 1] a) {:children true})]
      (for [[path {:keys [exprs?]}] (:children expr)
            :let [in (get-in expr path)]
            child-expr (if exprs?
                         in
                         [in])]
        child-expr))
    clojure.pprint/pprint)

  (def in (Compiler/analyze Compiler$C/STATEMENT '(seq 1)))
  (class in)
  (def method (doto (.getMethod (class in) "eval" (into-array Class []))
                (.setAccessible true)))
  (try (.invoke method in (object-array []))
    (catch java.lang.reflect.InvocationTargetException e
      (throw (repl/root-cause e))))
    )

