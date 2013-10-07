;:   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.emit-form)

(defmulti -emit-form (fn [{:keys [op]} _] op))

(defn emit-form [ast]
  (-emit-form ast false))

(defn emit-hygienic-form [ast]
  (-emit-form ast true))

(defmethod -emit-form :local
  [{:keys [name form]} hygienic?]
  (if hygienic? name form))

(defmethod -emit-form :binding
  [{:keys [name form]} hygienic?]
  (if hygienic? name form))

(defmethod -emit-form :var
  [{:keys [name form]} hygienic?]
  (if hygienic? name form))

(defn emit-bindings [bindings hygienic?]
  (mapcat (fn [{:keys [name form init]}]
            [(if hygienic? name form) (-emit-form init hygienic?)])
          bindings))

(defmethod -emit-form :letfn
  [{:keys [bindings body]} hygienic?]
  `(letfn* [~@(emit-bindings bindings hygienic?)]
           ~(-emit-form body hygienic?)))

(defmethod -emit-form :let
  [{:keys [bindings body]} hygienic?]
  `(let* [~@(emit-bindings bindings hygienic?)]
           ~(-emit-form body hygienic?)))

(defmethod -emit-form :loop
  [{:keys [bindings body]} hygienic?]
  `(loop* [~@(emit-bindings bindings hygienic?)]
           ~(-emit-form body hygienic?)))

(defmethod -emit-form :const
  [{:keys [form]} _]
  form)

(defmethod -emit-form :quote
  [{:keys [expr]} hygienic?]
  (list 'quote (-emit-form expr hygienic?)))

(defmethod -emit-form :vector
  [{:keys [items]} hygienic?]
  (mapv #(-emit-form % hygienic?) items))

(defmethod -emit-form :set
  [{:keys [items]} hygienic?]
  (set (mapv #(-emit-form % hygienic?) items)))

(defmethod -emit-form :map
  [{:keys [keys vals]} hygienic?]
  (apply hash-map (interleave (mapv #(-emit-form % hygienic?) keys)
                              (mapv #(-emit-form % hygienic?) vals))))

(defmethod -emit-form :with-meta
  [{:keys [expr meta]} hygienic?]
  (with-meta (-emit-form expr hygienic?)
    (-emit-form meta hygienic?)))

(defmethod -emit-form :do
  [{:keys [ret statements]} hygienic?]
  (if (seq statements)
    `(do ~@(mapv #(-emit-form % hygienic?) statements)
         ~(-emit-form ret hygienic?))
    (-emit-form ret hygienic?)))

(defmethod -emit-form :if
  [{:keys [test then else]} hygienic?]
  `(if ~(-emit-form test hygienic?)
     ~(-emit-form then hygienic?)
     ~@(when else [(-emit-form else hygienic?)])))

(defmethod -emit-form :new
  [{:keys [class args]} hygienic?]
  `(new ~(-emit-form class hygienic?) ~@(mapv #(-emit-form % hygienic?) args)))

(defmethod -emit-form :set!
  [{:keys [target val]} hygienic?]
  `(set! ~(-emit-form target hygienic?) ~(-emit-form val hygienic?)))

(defmethod -emit-form :try
  [{:keys [body catches finally]} hygienic?]
  `(try ~(-emit-form body hygienic?)
        ~@(mapv #(-emit-form % hygienic?) catches)
        ~@(when finally
            [`(finally ~(-emit-form finally hygienic?))])))

(defmethod -emit-form :catch
  [{:keys [class local body]} hygienic?]
  `(catch ~class ~(-emit-form local hygienic?)
     ~(-emit-form body hygienic?)))

(defmethod -emit-form :throw
  [{:keys [exception]} hygienic?]
  `(throw ~(-emit-form exception hygienic?)))

(defmethod -emit-form :recur
  [{:keys [exprs]} hygienic?]
  `(recur ~@(mapv #(-emit-form % hygienic?) exprs)))

(defmethod -emit-form :fn-method
  [{:keys [variadic? params body]} hygienic?]
  (let [params (if variadic? (concat (butlast params) '[&] (last params)) params)]
    `(~(mapv #(-emit-form % hygienic?) params)
      ~(-emit-form body hygienic?))))

(defmethod -emit-form :fn
  [{:keys [local methods]} hygienic?]
  `(fn* ~@(when local [(-emit-form local hygienic?)])
        ~@(mapv #(-emit-form % hygienic?) methods)))

(defmethod -emit-form :def
  [{:keys [name doc init]} hygienic?]
  `(def ~name ~@[doc] ~@(when init [(-emit-form init hygienic?)])))

(defmethod -emit-form :invoke
  [{:keys [fn args]} hygienic?]
  `(~(-emit-form fn hygienic?)
    ~@(mapv #(-emit-form % hygienic?) args)))

(defmethod -emit-form :monitor-enter
  [{:keys [target]} hygienic?]
  `(monitor-enter ~target))

(defmethod -emit-form :monitor-exit
  [{:keys [target]} hygienic?]
  `(monitor-exit ~target))

(defmethod -emit-form :import
  [{:keys [class]} hygienic?]
  `(clojure.core/import* ~(.getName ^Class class)))

(defmethod -emit-form :class
  [{:keys [class]} hygienic?]
  class)

(defmethod -emit-form :new
  [{:keys [class]} hygienic?]
  `(new ~class))

(defmethod -emit-form :the-var
  [{:keys [^clojure.lang.Var var]} hygienic?]
  `(var ~(symbol (name (ns-name (.ns var))) (name (.sym var)))))

(defmethod -emit-form :method
  [{:keys [params body this name]} hygienic?]
  (let [params (into [this] params)]
    `(~name ~(mapv #(-emit-form % hygienic?) params)
            ~(-emit-form body hygienic?))))

(defmethod -emit-form :deftype
  [{:keys [name class-name fields interfaces methods]} hygienic?]
  `(deftype* ~name ~class-name ~(mapv #(-emit-form % hygienic?) fields)
     :implements ~(vec interfaces)
     ~@(mapv #(-emit-form % hygienic?) methods)))

(defmethod -emit-form :reify
  [{:keys [interfaces methods]} hygienic?]
  `(reify* ~(vec interfaces)
           ~@(mapv #(-emit-form % hygienic?) methods)))

(defmethod -emit-form :case
  [{:keys [test default tests thens shift mask low high switch-type test-type skip-check?]} hygienic?]
  `(case* ~(-emit-form test hygienic?)
          ~shift ~mask
          ~(-emit-form default hygienic?)
          ~(apply sorted-map
                  (mapcat (fn [{:keys [hash test]} {:keys [then]}]
                            [hash [(-emit-form test hygienic?) (-emit-form then hygienic?)]])
                          tests thens))
          ~switch-type ~test-type ~skip-check?))

(defmethod -emit-form :static-field
  [{:keys [class field]} hygienic?]
  (symbol (.getName ^Class class) (name field)))

(defmethod -emit-form :static-call
  [{:keys [class method args]} hygienic?]
  `(~(symbol (.getName ^Class class) (name method))
    ~@(mapv #(-emit-form % hygienic?) args)))

(defmethod -emit-form :instance-field
  [{:keys [instance field]} hygienic?]
  `(~(symbol (str ".-" (name field))) ~(-emit-form instance hygienic?)))

(defmethod -emit-form :instance-call
  [{:keys [instance method args]} hygienic?]
  `(~(symbol (str "." (name method))) ~(-emit-form instance hygienic?)
    ~@(mapv #(-emit-form % hygienic?) args)))

(defmethod -emit-form :host-interop
  [{:keys [target m-or-f]} hygienic?]
  `(~(symbol (str "." (name m-or-f))) ~(-emit-form target hygienic?)))
