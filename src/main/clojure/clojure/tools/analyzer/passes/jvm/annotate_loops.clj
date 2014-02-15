;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.annotate-loops
  (:require [clojure.tools.analyzer.ast :refer [update-children]]))

(defmulti annotate-loops :op)

(defn many [ast]
  (assoc ast :times :many))

(defmethod annotate-loops :do
  [{:keys [statements ret times] :as ast}]
  (if (or (= :recur (:op ret))
          (= times :many))
    (assoc ast
      :ret        (assoc ret :times :many)
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
