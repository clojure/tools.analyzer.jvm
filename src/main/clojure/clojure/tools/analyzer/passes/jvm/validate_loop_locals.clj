;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.validate-loop-locals
  (:require [clojure.tools.analyzer.ast :refer [prewalk postwalk children update-children]]
            [clojure.tools.analyzer.jvm.utils :refer [wider-tag maybe-class]]))

(def ^:dynamic ^:private validating? false)
(def ^:dynamic ^:private mismatch?)

(defn find-mismatch [{:keys [op exprs] :as ast} tags loop-id]
  (when (and (= op :recur) (= loop-id (:loop-id ast))
             (not= (mapv :tag exprs) tags))
    (swap! mismatch? conj (mapv :tag exprs)))
  ast)

(defmulti -validate-loop-locals (fn [_ {:keys [op]}] op))
(defmulti -cleanup-dirty-nodes :op)

(defmethod -cleanup-dirty-nodes :local
  [{:keys [form atom env] :as ast}]
  (if-let [cast ((:loop-locals-casts env) form)]
    (assoc ast
      :dirty? true
      :bind-tag cast
      :tag (or (:tag (meta form)) cast)
      :form (if (:tag (meta form))
              (vary-meta form assoc :tag (symbol (.getName ^Class cast)))
              form))
    (if (and (:dirty? @atom)
             (not (:tag (meta form))))
      (dissoc (assoc ast :dirty? true) :bind-tag :tag)
      ast)))

(defn dirty [ast]
  (when-let [atom (:atom ast)]
    (swap! atom assoc :dirty? true))
  (assoc (update-children ast (fn [ast] (dissoc ast :dirty?)))
    :dirty? true))

(defmethod -cleanup-dirty-nodes :do
  [{:keys [op ret] :as ast}]
  (if (:dirty? ret)
    (dissoc (dirty ast) :tag)
    ast))

;; should check for :tag meta form
(defmethod -cleanup-dirty-nodes :default
  [{:keys [op] :as ast}]
  (if (some :dirty? (children ast))
    (dissoc (dirty ast)
            :tag :validated? :ret-tag (when (= :instance-call op) :class))
    ast))

(defn -validate-loop-locals*
  [analyze {:keys [body env loop-id] :as ast} key]
  (if validating?
    ast
    (binding [mismatch? (atom #{})]
      (let [bindings (key ast)
            bind-tags (mapv :tag bindings)]
        (prewalk body (fn [ast] (find-mismatch ast bind-tags loop-id)))
        (if-let [mismatches (seq @mismatch?)]
          (let [bindings (apply mapv
                                (fn [{:keys [form tag]} & mismatches]
                                  (if (every? #{tag} mismatches)
                                    form
                                    (let [tags (conj mismatches tag)]
                                      (with-meta form {:tag (or (wider-tag tags) Object)}))))
                                bindings mismatches)
                binds (zipmap bindings (mapv (comp maybe-class :tag meta) bindings))
                analyze* (fn [ast]
                           (analyze (postwalk ast
                                              (fn [ast]
                                                (when-let [atom (:atom ast)]
                                                  (swap! atom dissoc :dirty?))
                                                ast))))]
            (binding [validating? true]
              (analyze* (dissoc (postwalk (assoc ast key
                                                 (mapv (fn [{:keys [atom] :as bind} f]
                                                         (swap! atom assoc :dirty? true)
                                                         (assoc (dissoc bind :tag) :form f))
                                                       (key ast) bindings))
                                          (comp -cleanup-dirty-nodes
                                             (fn [ast] (assoc-in ast [:env :loop-locals-casts] binds))))
                                :dirty?))))
          ast)))))

(defmethod -validate-loop-locals :loop
  [analyze ast]
  (-validate-loop-locals* analyze ast :bindings))

(defmethod -validate-loop-locals :fn-method
  [analyze ast]
  (-validate-loop-locals* analyze ast :params))

(defmethod -validate-loop-locals :method
  [analyze ast]
  (-validate-loop-locals* analyze ast :params))

(defmethod -validate-loop-locals :recur
  [_ {:keys [exprs env] :as ast}]
  (if validating?
    (let [casts (:loop-locals-casts env)
          locals (:loop-locals env)]
      (assoc ast
        :exprs (mapv (fn [{:keys [env] :as e} n]
                       (if-let [[form c] (find casts n)]
                         (assoc e :tag c :form form)
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
