;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.validate
  (:require [clojure.tools.analyzer :refer [-analyze]]
            [clojure.tools.analyzer.ast :refer [prewalk]]
            [clojure.tools.analyzer.passes.cleanup :refer [cleanup]]
            [clojure.tools.analyzer.utils :refer [arglist-for-arity source-info]]
            [clojure.tools.analyzer.jvm.utils :as u])
  (:import (clojure.lang IFn ExceptionInfo)))

(defmulti -validate :op)

(defmethod -validate :maybe-class
  [{:keys [class form env] :as ast}]
  (let [{:keys [ns namespaces]} env]
    (if-let [the-class (or (u/maybe-class class)
                           (u/maybe-class (-> @namespaces (get ns) :mappings (get class))))]
      (assoc (-analyze :const the-class env :class)
        :tag  Class
        :form form)
      (if (.contains (str class) ".") ;; try and be smart for the exception
        (throw (ex-info (str "Class not found: " class)
                        (merge {:class class}
                               (source-info env))))
        (throw (ex-info (str "Could not resolve var: " class)
                        (merge {:var class}
                               (source-info env))))))))

(defmethod -validate :maybe-host-form
  [{:keys [class form env]}]
  (throw (ex-info (str "No such namespace: " class)
                  (merge {:ns   class
                          :form form}
                         (source-info env)))))

(defmethod -validate :catch
  [{:keys [class validated? env form] :as ast}]
  (if-not validated?
    (if-let [the-class (u/maybe-class class)]
      (assoc ast
        :class the-class
        :validated? true)
      (throw (ex-info (str "Class not found: " class)
                      (merge {:class class
                              :form  form}
                             (source-info env)))))
    ast))

(defmethod -validate :set!
  [{:keys [target form env] :as ast}]
  (when (and (not (:assignable? target))
             (not (= :host-interop (:op target))))
    (throw (ex-info "Cannot set! non-assignable target"
                    (merge {:target (prewalk target cleanup)
                            :form   form}
                           (source-info env)))))
  ast)

(defn tag-match? [arg-tags meth]
  (every? identity (map u/convertible? arg-tags (:parameter-types meth))))

(defn try-best-match [tags methods]
  (let [o-tags (mapv #(or (u/maybe-class %) Object) tags)
        exact-matches (seq (filter
                            #(= o-tags (mapv u/maybe-class  (:parameter-types %))) methods))]
    (if exact-matches
      (if (next exact-matches)
        [(reduce (fn [prev next]
                   (if (.isAssignableFrom (u/maybe-class (:return-type prev))
                                          (u/maybe-class (:return-type next)))
                     next
                     prev)) exact-matches)]
        exact-matches)
      (if-let [methods (seq (filter #(tag-match? tags %) methods))]
        (reduce (fn [[prev & _ :as p] next]
                  (if (or (not prev)
                          (and (= (mapv u/maybe-class (:parameter-types prev))
                                  (mapv u/maybe-class (:parameter-types next)))
                               (.isAssignableFrom (u/maybe-class (:return-type prev))
                                                  (u/maybe-class (:return-type next))))
                          (some true? (mapv u/subsumes? (:parameter-types next)
                                         (:parameter-types prev))))
                    [next]
                    (conj p next))) [] methods)
        methods))))

(defn validate-class
  [{:keys [class env] :as ast}]
  (if-let [the-class (u/maybe-class class)]
    (assoc ast :class the-class)
    (throw (ex-info (str "Class not found: " class)
                    (merge {:class class}
                           (source-info env))))))

(defmethod -validate :new
  [{:keys [validated? env] :as ast}]
  (if validated?
    ast
    (let [{:keys [args ^Class class] :as ast} (validate-class ast)
          c-name (symbol (.getName class))
          argc (count args)
          tags (mapv :tag args)]
      (let [[ctor & rest] (->> (filter #(= (count (:parameter-types %)) argc)
                                       (u/members class c-name))
                               (try-best-match tags))]
        (if ctor
          (if (empty? rest)
            (let [arg-tags (mapv u/maybe-class (:parameter-types ctor))
                  args (mapv (fn [arg tag] (assoc arg :tag tag))
                             args arg-tags)]
              (assoc ast
                :args       args
                :validated? true))
            ast)
          (throw (ex-info (str "no ctor found for ctor of class: " class " and give signature")
                          (merge {:class class
                                  :args  (mapv (fn [a] (prewalk a cleanup)) args)}
                                 (source-info env)))))))))

(defn validate-call [{:keys [class method args tag env op] :as ast}]
  (let [argc (count args)
        instance? (= :instance-call op)
        f (if instance? u/instance-methods u/static-methods)
        tags (mapv :tag args)]
    (if-let [matching-methods (seq (f class method argc))]
      (let [[m & rest :as matching] (try-best-match tags matching-methods)]
        (if m
          (if (empty? rest)
            (let [ret-tag  (u/maybe-class (:return-type m))
                  arg-tags (mapv u/maybe-class (:parameter-types m))
                  args (mapv (fn [arg tag] (assoc arg :tag tag)) args arg-tags)]
              (assoc ast
                :method     (:name m)
                :validated? true
                :ret-tag    ret-tag
                :tag        (or tag ret-tag)
                :args       args))
            (if (apply = (mapv (comp u/maybe-class :return-type) matching))
              (let [ret-tag (u/maybe-class (:return-type m))]
                (assoc ast
                  :ret-tag Object
                  :tag     (or tag ret-tag)))
              ast))
          (if instance?
            (dissoc ast :class :tag)
            (throw (ex-info (str "No matching method: " method " for class: " class " and given signature")
                            (merge {:method method
                                    :class  class
                                    :args   (mapv (fn [a] (prewalk a cleanup)) args)}
                                   (source-info env)))))))
      (if instance?
        (dissoc ast :class :tag)
        (throw (ex-info (str "No matching method: " method " for class: " class " and arity: " argc)
                        (merge {:method method
                                :class  class
                                :argc   argc}
                               (source-info env))))))))

(defmethod -validate :static-call
  [{:keys [validated?] :as ast}]
  (if validated?
    ast
    (validate-call  ast)))

(defmethod -validate :instance-call
  [{:keys [class validated? instance] :as ast}]
  (let [class (or class (u/maybe-class (:tag instance)))]
    (if (and class (not validated?))
      (validate-call (assoc ast :class class))
      ast)))

(defmethod -validate :import
  [{:keys [class validated? env form] :as ast}]
  (if-not validated?
    (if-let [the-class (u/maybe-class class)]
      (let [{:keys [ns namespaces]} env
            class-name (.getName the-class)
            class-sym (-> class-name (subs (inc (.lastIndexOf class-name "."))) symbol)
            sym-val (get-in @namespaces [ns :mappings class-sym])]
        (if (and sym-val (not= (.getName ^Class sym-val) class-name)) ;; allow deftype redef
          (throw (ex-info (str class-sym " already refers to: " sym-val
                               " in namespace: " ns)
                          (merge {:class     class
                                  :class-sym class-sym
                                  :sym-val   sym-val
                                  :form      form}
                                 (source-info env))))
          (do
            (swap! namespaces assoc-in
                   [ns :mappings class-sym] the-class)
            (assoc ast :class the-class
                   :validated? true))))
      (throw (ex-info (str "Class not found: " class)
                      (merge {:class class
                              :form  form}
                             (source-info env)))))
    ast))

(defmethod -validate :def
  [{:keys [var init form env] :as ast}]
  #_(when-let [tag (:tag init)]
      (alter-meta! var assoc :tag tag))
  (when-let [arglists (:arglists init)]
    (doseq [arglist arglists]
      (when-let [tag (:tag (meta arglist))]
        (if-not (u/maybe-class tag)
          (throw (ex-info (str "Class: " tag " not found as tag for var: " var)
                          (merge {:tag-kind :var-metadata
                                  :class    tag
                                  :form     form}
                                 (source-info env)))))))
    #_(alter-meta! var assoc :arg-lists arglists))
  ast)

(defmethod -validate :invoke
  [{:keys [args tag env fn form] :as ast}]
  (let [argc (count args)]
    (when (and (= :const (:op fn))
               (not (instance? IFn (:form fn))))
      (throw (ex-info (str (class (:form fn)) " is not a function, but it's used as such")
                      (merge {:form form}
                             (source-info env)))))
    (if (and (:arglists fn)
             (not (arglist-for-arity fn argc)))
      (assoc ast :maybe-mismatch-arity true)
      #_(throw (ex-info (str "No matching arity found for function: " (:name fn))
                        {:arity (count args)
                         :fn    fn}))
      ast)))

(defn validate-interfaces [{:keys [env form interfaces]}]
  (when-not (every? #(.isInterface ^Class %) (disj interfaces Object))
    (throw (ex-info "only interfaces or Object can be implemented by deftype/reify"
                    (merge {:interfaces interfaces
                            :form       form}
                           (source-info env))))))

(defmethod -validate :deftype
  [{:keys [class-name] :as ast}]
  (validate-interfaces ast)
  (assoc ast :class-name (u/maybe-class class-name)))

(defmethod -validate :reify
  [{:keys [class-name] :as ast}]
  (validate-interfaces ast)
  (assoc ast :class-name (u/maybe-class class-name)))

(defmethod -validate :method
  [{:keys [name class interfaces methods tag form params fixed-arity env] :as ast}]
  (if interfaces
    (let [tags (mapv :tag params)
          methods-set (set (mapv (fn [x] (dissoc x :declaring-class :flags)) methods))]
      (let [[m & rest :as matches] (try-best-match tags methods)]
        (if m
          (let [ret-tag  (u/maybe-class (:return-type m))
                i-tag    (u/maybe-class (:declaring-class m))
                arg-tags (mapv u/maybe-class (:parameter-types m))
                params   (mapv (fn [arg tag] (assoc arg :tag tag)) params arg-tags)]
            (if (or (empty? rest)
                    (every? (fn [{:keys [return-type parameter-types]}]
                         (and (= (u/maybe-class return-type) ret-tag)
                              (= arg-tags (mapv u/maybe-class parameter-types)))) rest))
              (assoc (dissoc ast :interfaces :methods)
                :bridges   (filter (fn [{:keys [return-type]}]
                                     (.isAssignableFrom (u/maybe-class return-type)
                                                        ret-tag))
                                   (disj methods-set
                                         (dissoc m :declaring-class :flags)))
                :methods   methods
                :interface i-tag
                :tag       ret-tag
                :params    params)
              (throw (ex-info (str "Ambiguous method signature for method: " name)
                              (merge {:method     name
                                      :interfaces interfaces
                                      :form       form
                                      :params     (mapv (fn [x] (prewalk x cleanup)) params)
                                      :matches    matches}
                                     (source-info env))))))
          (throw (ex-info (str "No such method found: " name " with given signature in any of the"
                               " provided interfaces: " interfaces)
                          (merge {:method     name
                                  :methods    methods
                                  :interfaces interfaces
                                  :form       form
                                  :params     params}
                                 (source-info env)))))))
    ast))

(defmethod -validate :default [ast] ast)

(defn validate-tag [t {:keys [env] :as ast}]
  (let [tag (ast t)]
    (if-let [the-class (u/maybe-class tag)]
      {t the-class}
      (throw (ex-info (str "Class not found: " tag " as tag of kind: " t)
                      (merge {:class    tag
                              :tag-kind t}
                             (source-info env)))))))

(defn validate
  "Validate tags, classes, method calls.
   Throws exceptions when invalid forms are encountered, replaces
   class symbols with class objects."
  [{:keys [tag ret-tag bind-tag return-tag] :as ast}]
  (let [ast (merge ast
                   (when tag
                     (validate-tag :tag ast))
                   (when ret-tag
                     (validate-tag :ret-tag ast))
                   (when return-tag
                     (validate-tag :return-tag ast))
                   (when bind-tag
                     (validate-tag :bind-tag ast)))]
    (-validate ast)))
