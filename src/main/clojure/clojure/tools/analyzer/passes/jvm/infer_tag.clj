;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.infer-tag
  (:require [clojure.tools.analyzer.utils :refer [arglist-for-arity]]
            [clojure.tools.analyzer.jvm.utils :as u]))

(defmulti -infer-tag :op)
(defmethod -infer-tag :default [ast] ast)

(defmethod -infer-tag :binding
  [{:keys [init atom] :as ast}]
  (if init
    (let [info (select-keys init [:return-tag :arglists])]
      (swap! atom merge info)
      (merge ast info))
    ast))

(defmethod -infer-tag :local
  [{:keys [atom] :as ast}]
  (merge @atom
         ast
         {:o-tag (:tag @atom)}))

(defmethod -infer-tag :var
  [{:keys [var form] :as ast}]
  (let [{:keys [tag arglists]} (meta var)
        arglists (if (= 'quote (first arglists))
                   (second arglists)
                   arglists)
        form-tag (u/maybe-class (:tag (meta form)))]
    ;;if (not dynamic)
    (merge ast
           {:o-tag Object}
           (when-let [tag (or (u/maybe-class tag) form-tag)]
             (if (fn? @var)
               {:tag clojure.lang.AFunction :return-tag tag}
               {:tag tag}))
           (when arglists
             {:arglists arglists}))))

(defmethod -infer-tag :def
  [{:keys [init var] :as ast}]
  (merge (assoc ast :tag clojure.lang.Var :o-tag clojure.lang.Var)
         (select-keys init [:return-tag :arglists])))

(defmethod -infer-tag :new
  [{:keys [class] :as ast}]
  (let [t (u/maybe-class class)]
    (assoc ast :o-tag t :tag t)))

(defmethod -infer-tag :with-meta
  [{:keys [expr] :as ast}]
  (merge ast (select-keys expr [:return-tag :arglists])
         {:tag (or (:tag expr) Object) :o-tag Object})) ;;trying to be smart here

(defmethod -infer-tag :recur
  [ast]
  (assoc ast :loop-tag true))

(defmethod -infer-tag :do
  [{:keys [ret] :as ast}]
  (merge ast (select-keys ret [:return-tag :arglists :loop-tag :tag])
         {:o-tag (:tag ret)}))

(defmethod -infer-tag :let
  [{:keys [body] :as ast}]
  (merge ast (select-keys body [:return-tag :arglists :loop-tag :tag])
         {:o-tag (:tag body)}))

(defmethod -infer-tag :letfn
  [{:keys [body] :as ast}]
  (merge ast (select-keys body [:return-tag :arglists :loop-tag :tag])
         {:o-tag (:tag body)}))

(defmethod -infer-tag :loop
  [{:keys [body] :as ast}]
  (merge ast (select-keys body [:return-tag :arglists :tag])
         {:o-tag (:tag body)}))

(defmethod -infer-tag :if
  [{:keys [then else] :as ast}]
  (let [then-tag (:tag then)
        else-tag (:tag else)]
    (cond
     (and then-tag
          (or (:loop-tag else)
              (= then-tag else-tag)))
     (merge ast
            {:tag then-tag :o-tag then-tag}
            (when-let [return-tag (:return-tag then)]
              (when (= return-tag (:return-tag else)) ;;FIX: could fail when (:loop-tag else)
                {:return-tag return-tag}))
            (when-let [arglists (:arglists then)] ;;FIX: could fail when (:loop-tag else)
              (when (= arglists (:arglists else)) ;;FIX: should check meta
                {:arglists arglists})))

     (and else-tag (:loop-tag then))
     (merge ast
            {:tag else-tag :o-tag else-tag}
            (when-let [return-tag (:return-tag else)]
              {:return-tag return-tag})
            (when-let [arglists (:arglists else)]
              {:arglists arglists}))

     (and (:loop-tag then) (:loop-tag else))
     (assoc ast :loop-tag true)

     :else
     ast)))

(defmethod -infer-tag :case
  [{:keys [thens default] :as ast}]
  (let [thens (conj (mapv :then thens) default)
        exprs (seq (remove :loop-tag thens))
        tag (:tag (first exprs))]
    (cond
     (and tag
          (every? #(= (:tag %) tag) exprs))
     (merge ast
            {:tag tag :o-tag tag}
            (when-let [return-tag (:return-tag (first exprs))]
              (when (every? #(= (:return-tag %) return-tag) exprs)
                {:return-tag return-tag}))
            (when-let [arglists (:arglists (first exprs))]
              (when (every? #(= (:arglists %) arglists) exprs) ;;FIX: should check meta
                {:arglists arglists})))

     (every? :loop-tag thens)
     (assoc ast :loop-tag true)

     :else
     ast)))

(defmethod -infer-tag :try
  [{:keys [body catches] :as ast}]
  (let [{:keys [tag return-tag arglists]} body]
    (merge ast
           (when (and tag (every? #(= % tag) (mapv (comp :tag :body) catches)))
             {:tag tag :o-tag tag})
           (when (and return-tag (every? #(= % return-tag) (mapv (comp :return-tag :body) catches)))
             {:return-tag return-tag})
           (when (and arglists (every? #(= % arglists) (mapv (comp :arglists :body) catches))) ;;FIX: should check meta
             {:arglists arglists}))))

(defmethod -infer-tag :fn-method
  [{:keys [form body params local] :as ast}]
  (let [annotated-tag (or (:tag (meta (first form)))
                          (:tag (meta (:form local))))
        body-tag (:tag body)
        tag (or annotated-tag body-tag)]
    (merge ast
           (when tag
             {:tag   (or body-tag tag)
              :o-tag tag})
           {:arglist (with-meta (vec (mapcat (fn [{:keys [form variadic?]}]
                                               (if variadic? ['& form] [form]))
                                             params))
                       (when tag {:tag tag}))})))

(defmethod -infer-tag :fn
  [{:keys [local methods] :as ast}]
  (merge ast
         {:arglists (seq (map :arglist methods))
          :tag      clojure.lang.AFunction
          :o-tag    clojure.lang.AFunction}
         (when-let [tag (:tag (meta (:form local)))]
           {:return-tag tag})))

(defmethod -infer-tag :invoke
  [{:keys [fn args] :as ast}]
  (if (:arglists fn)
    (let [argc (count args)
          arglist (arglist-for-arity fn argc)
          tag (or (:tag (meta arglist))
                  (:return-tag fn)
                  (and (= :var (:op fn))
                       (:tag (meta (:var fn)))))]
      (merge ast
             (when tag
               {:tag     tag
                :o-tag   tag})))
    (if-let [tag (:return-tag fn)]
      (assoc ast :tag tag :o-tag tag)
      ast)))

(defmethod -infer-tag :method
  [{:keys [form body params] :as ast}]
  (let [tag (or (:tag (meta (first form)))
                (:tag (meta (second form))))
        body-tag (:tag body)]
    (assoc ast :tag (or tag body-tag) :o-tag body-tag)))

(defmethod -infer-tag :reify
  [{:keys [class-name] :as ast}]
  (assoc ast :tag class-name :o-tag class-name))

(defmethod -infer-tag :set!
  [{:keys [target] :as ast}]
  (let [t (:tag target)]
    (assoc ast :tag t :o-tag t)))

(defn infer-tag
  "Performs local type inference on the AST"
  [{:keys [tag form] :as ast}]
  (if-let [tag (or tag (:tag (meta form)))]
    (assoc (-infer-tag ast) :tag tag)
    (-infer-tag ast)))
