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

(defn many [ast]
  (assoc ast :times :many))

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

(defmethod annotate-loops :do
  [{:keys [statements ret times] :as ast}]
  (if (or (= times :many)
          (has-recur? ret))
    (assoc ast
      :ret        (many ret)
      :statements (mapv many statements))
    ast))

(defmethod annotate-loops :recur
  [ast]
  (update-children ast many))

(defmethod annotate-loops :default
  [ast]
  (if (= (:times ast) :many)
    (update-children ast many)
    ast))

(defmethod annotate-loops :if
  [{:keys [test then else] :as ast}]
  (if (= (:times ast) :many)
    (let [then (if (has-recur? then)
                 (many then)
                 then)
          else (if (has-recur? else)
                 (many else)
                 else)]
      (assoc ast :then then :else else :test (many test)))
    ast))

(defmethod annotate-loops :case
  [{:keys [test default thens] :as ast}]
  (if (= (:times ast) :many)
    (let [default (if (has-recur? default)
                    (many default)
                    default)

          thens (mapv #(if (has-recur? %)
                         (many %)
                         %) thens)]
      (assoc ast :thens thens :default default :test (many test)))
    ast))
