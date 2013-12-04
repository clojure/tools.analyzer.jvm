;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.validate-loop-locals
  (:require [clojure.tools.analyzer.passes :refer [prewalk postwalk]]
            [clojure.tools.analyzer.utils :refer [update!]]
            [clojure.tools.analyzer.jvm.utils :refer [wider-tag]]))

(def ^:dynamic ^:private validating? false)
(def ^:dynamic ^:private mismatch? #{})

(defn find-mismatch [{:keys [op exprs] :as ast} tags]
  (when (and (= op :recur)
             (not= (mapv :tag exprs) tags))
    (update! mismatch? conj (mapv :tag exprs)))
  ast)

(defmulti -validate-loop-locals (fn [_ {:keys [op]}] op))

(defn -validate-loop-locals*
  [analyze {:keys [body env] :as ast} key]
  (if validating?
    ast
    (binding [mismatch? #{}]
      (let [bindings (key ast)
            bind-tags (mapv :tag bindings)]
        (prewalk body (fn [ast] (find-mismatch ast bind-tags)))
        (if (seq mismatch?)
          (let [bindings (apply mapv
                                (fn [{:keys [form tag]} & mismatches]
                                  (if (every? #{tag} mismatches)
                                    form
                                    (let [tags (conj mismatches tag)]
                                      (with-meta form {:tag (or (wider-tag tags) Object)}))))
                                bindings mismatch?)
                binds (zipmap bindings (mapv (comp :tag meta) bindings))]
            (binding [validating? true]
              (postwalk (prewalk (assoc ast key
                                        (mapv (fn [bind f]
                                                (assoc bind :form f))
                                              (key ast) bindings))
                                 (fn [ast]
                                   (assoc-in (dissoc ast :tag :validated? :ret-tag :bind-tag)
                                             [:env :loop-locals-casts] binds)))
                        analyze)))
          ast)))))

(defmethod -validate-loop-locals :loop
  [analyze ast]
  (-validate-loop-locals* analyze ast :bindings))

(defmethod -validate-loop-locals :fn-method
  [analyze ast]
  (-validate-loop-locals* analyze ast :params))

(defmethod -validate-loop-locals :local
  [_ {:keys [form local env] :as ast}]
  (if validating?
    (if-let [cast ((:loop-locals-casts env) form)]
      (assoc ast :bind-tag cast)
      ast)
    ast))

(defmethod -validate-loop-locals :recur
  [_ {:keys [exprs env] :as ast}]
  (if validating?
    (let [casts (:loop-locals-casts env)
          locals (:loop-locals env)]
      (assoc ast
        :exprs (mapv (fn [{:keys [env] :as e} n]
                       (if-let [c (get casts n)]
                         (assoc e :tag c)
                         e)) exprs locals)))
    ast))

(defmethod -validate-loop-locals :default
  [_ ast]
  ast)

(defn validate-loop-locals
  "Returns a pass that validates the loop locals, calling analyze on the loop AST when
   a mismatched loop-local is found"
  [analyze]
  (fn [ast] (-validate-loop-locals analyze ast)))
