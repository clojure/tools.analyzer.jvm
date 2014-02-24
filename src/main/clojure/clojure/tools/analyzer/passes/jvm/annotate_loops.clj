;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.annotate-loops
  (:require [clojure.tools.analyzer.ast :refer [update-children]]))

;; TODO: optimize
(defmulti annotate-loops :op)
(defmulti has-recur? :op)

(defmethod has-recur? :do
  [ast]
  (has-recur? (:ret ast)))

(defmethod has-recur? :let
  [ast]
  (has-recur? (:body ast)))

(defmethod has-recur? :letfn
  [ast]
  (has-recur? (:body ast)))

(defmethod has-recur? :if
  [ast]
  (or (has-recur? (:then ast))
      (has-recur? (:else ast))))

(defmethod has-recur? :case
  [ast]
  (or (has-recur? (:default ast))
      (some has-recur? (:thens ast))))

(defmethod has-recur? :recur
  [_]
  true)

(defmethod has-recur? :default
  [_]
  false)

(defn -loops [ast loop-id]
  (update-in ast [:loops] (fnil conj #{}) loop-id))

(defmethod annotate-loops :loop
  [{:keys [body loops loop-id] :as ast}]
  (let [ast (if loops
              (update-children ast #(assoc % :loops loops))
              ast)]
    (if (has-recur? body)
      (update-in ast [:body] -loops loop-id)
      ast)))

(defmethod annotate-loops :default
  [{:keys [loops] :as ast}]
  (if loops
    (update-children ast #(assoc % :loops loops))
    ast))

(defmethod annotate-loops :if
  [{:keys [loops test then else env] :as ast}]
  (if loops
    (let [loop-id (:loop-id env)
          loops-no-recur (disj loops loop-id)
          then (if (has-recur? then)
                 (assoc then :loops loops)
                 (assoc then :loops loops-no-recur))
          else (if (has-recur? else)
                 (assoc else :loops loops)
                 (assoc else :loops loops-no-recur))]
      (assoc ast
        :then then
        :else else
        :test (assoc test :loops loops)))
    ast))

(defmethod annotate-loops :case
  [{:keys [loops test default thens env] :as ast}]
  (if loops
    (let [loop-id (:loop-id env)
          loops-no-recur (disj loops loop-id)
          default (if (has-recur? default)
                    (assoc default :loops loops)
                    (assoc default :loops loops-no-recur))

          thens (mapv #(if (has-recur? %)
                         (assoc % :loops loops)
                         (assoc % :loops loops-no-recur)) thens)]
      (assoc ast
        :thens   thens
        :default default
        :test    (assoc test :loops loops)))
    ast))
