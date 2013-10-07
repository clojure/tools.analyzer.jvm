;:   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.box
  (:require [clojure.tools.analyzer.jvm.utils :as u])
  (:require [clojure.tools.analyzer.utils :refer [protocol-node? arglist-for-arity]]))

(defmulti box :op)

(defmacro if-let-box [class then else]
  `(let [c# ~class
         ~class (u/box c#)]
     (if (u/primitive? c#)
       ~then
       ~else)))

(defn -box [{:keys [tag] :as ast}]
  (if (u/primitive? tag)
    (assoc ast :tag (u/box tag))
    ast))

(defn boxed? [tag expr]
  (and (not (u/primitive? tag))
       (u/primitive? (:tag expr))))

(defmethod box :instance-call
  [{:keys [args class validated?] :as ast}]
  (let [ast (if-let-box class
              (assoc (update-in ast [:instance :tag] u/box) :class class)
              ast)]
    (if validated?
      ast
      (assoc ast :args (mapv -box args)))))

(defmethod box :static-call
  [{:keys [args validated?] :as ast}]
  (if validated?
    ast
    (assoc ast :args (mapv -box args))))

(defmethod box :new
  [{:keys [args validated?] :as ast}]
  (if validated?
    ast
    (assoc ast :args (mapv -box args))))

(defmethod box :instance-field
  [{:keys [class] :as ast}]
  (if-let-box class
    (assoc (update-in ast [:instance :tag] u/box) :class class)
    ast))

(defmethod box :def
  [{:keys [init] :as ast}]
  (if (and init (u/primitive? (:tag init)))
    (update-in ast [:init] -box)
    ast))

(defmethod box :vector
  [{:keys [items] :as ast}]
  (assoc ast :items (mapv -box items)))

(defmethod box :set
  [{:keys [items] :as ast}]
  (assoc ast :items (mapv -box items)))

(defmethod box :map
  [{:keys [keys vals] :as ast}]
  (let [keys (mapv -box keys)
        vals (mapv -box vals)]
    (assoc ast
      :keys keys
      :vals vals)))

(defmethod box :do
  [{:keys [tag ret] :as ast}]
  (if (boxed? tag ret)
    (update-in ast [:ret] -box)
    ast))

(defmethod box :quote
  [{:keys [tag expr] :as ast}]
  (if (boxed? tag expr)
    (update-in ast [:expr] -box)
    ast))

(defmethod box :keyword-invoke
  [{:keys [args] :as ast}]
  (assoc ast :args (mapv -box args)))

(defmethod box :let
  [{:keys [tag body] :as ast}]
  (if (boxed? tag body)
    (update-in ast [:body] -box)
    ast))

(defmethod box :letfn
  [{:keys [tag body] :as ast}]
  (if (boxed? tag body)
    (update-in ast [:body] -box)
    ast))

(defmethod box :loop
  [{:keys [tag body] :as ast}]
  (if (boxed? tag body)
    (update-in ast [:body] -box)
    ast))

(defmethod box :fn-method
  [{:keys [tag] :as  ast}]
  (if (u/primitive? tag)
    ast
    (-box (update-in ast [:body] -box))))

(defmethod box :if
  [{:keys [test then else tag] :as ast}]
  (let [test-tag (:tag test)
        test (if (and (u/primitive? test-tag)
                      (not= Boolean/TYPE test-tag))
               (assoc test :tag (u/box test-tag))
               test)
        [then else] (if (or (boxed? tag then)
                            (boxed? tag else))
                      (mapv -box [then else])
                      [then else])]
    (merge ast
           {:test test
            :then then
            :else else})))

(defmethod box :case
  [{:keys [tag default tests thens] :as ast}]
  (if (and tag (u/primitive? tag))
    ast
    (-> ast
      (assoc-in [:thens] (mapv (fn [t] (update-in t [:then] -box)) thens))
      (update-in [:default] -box))))

(defmethod box :try
  [ast]
  (-> ast
    (update-in [:body] -box)
    (update-in [:catches] #(mapv -box %))
    (update-in [:finally] -box)))

(defmethod box :invoke
  [{:keys [fn args] :as ast}]
  (assoc ast
    :args (mapv -box args)
    :ret-tag Object))

(defmethod box :default [ast] ast)
