;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.clear-locals
  (:require [clojure.tools.analyzer.ast :refer [update-children rseqv]]))

(def ^:dynamic *clears*)

(defmulti -clear-locals :op)

(defn maybe-clear-local
  [{:keys [name local should-not-clear env times] :as ast}]
  (let [{:keys [closed-overs locals loop-closed-overs]} @*clears*]
    (if (and (#{:let :loop :catch :arg} local)
             (or (not (loop-closed-overs name)) ;; if we're in a loop and the local is defined outside the loop
                 (not= :many times))             ;; it's only safe to clear it if we're in the loop exit path
             (or (not (closed-overs name)) ;; if it's a closed-over var, we can only clear it if we explicitely
                 (:once env))            ;; declared the function to be run :once
             (not (locals name)) ;; if the local is in `locals` it means that it's used later in the body and can't be cleared here
             (not should-not-clear)) ;; letfn bindings/case test
      (assoc ast :to-clear? true)
      ast)))

(defn maybe-clear-this
  [{:keys [env] :as ast}]
  (-> (if (and (= :return (:context env))
              (not (:in-try env)))
       (assoc ast :to-clear? true)
       ast)
    (update-children -clear-locals rseqv)))

(defmethod -clear-locals :invoke
  [ast]
  (maybe-clear-this ast))

(defmethod -clear-locals :protocol-invoke
  [ast]
  (maybe-clear-this ast))

(defmethod -clear-locals :prim-invoke
  [ast]
  (maybe-clear-this ast))

(defmethod -clear-locals :static-call
  [ast]
  (maybe-clear-this ast))

(defmethod -clear-locals :instance-call
  [ast]
  (maybe-clear-this ast))

(defmethod -clear-locals :default
  [{:keys [closed-overs op] :as ast}]
  (if closed-overs
    (let [key (if (= :loop op) :loop-closed-overs :closed-overs) ;; if we're in a loop those are not actually closed-overs
          [ast body-locals] (binding [*clears* (atom (update-in @*clears* [key] merge closed-overs))] ;; clear locals in the body
                              [(update-children ast -clear-locals rseqv) (:locals @*clears*)])        ;; and save encountered locals
          [ks vs] (reduce-kv (fn [[keys vals] k v]
                               [(conj keys k) (conj vals v)])
                             [[] []] closed-overs)
          closed-overs (zipmap ks (mapv maybe-clear-local vs))]  ;; clear outer closed-overs at the point of the closure creation
      (swap! *clears* #(update-in % [:locals] into body-locals)) ;; merge the locals so that we know not to clear them "before"
      (if (#{:fn :reify} op)
        (assoc ast :closed-overs closed-overs)
        ast))
    (update-children ast -clear-locals rseqv)))

(defmethod -clear-locals :if
  [{:keys [test then else] :as ast}]
  (let [[then then-clears] (binding [*clears* (atom @*clears*)] ;; push a new locals frame for every path so that
                             [(-clear-locals then) @*clears*])  ;; we can clear the same local in different branches
        [else else-clears] (binding [*clears* (atom @*clears*)] ;; this is safe to do since the different paths will
                             [(-clear-locals else) @*clears*])  ;; never interfere
        locals             (into (:locals then-clears)    ;; merge all the locals encountered in the branch paths
                                 (:locals else-clears))]  ;; so that if we encounter them "before" in the body we know
    (swap! *clears* #(update-in % [:locals] into locals)) ;; that we cannot clear them since they are needed later
    (let [test (-clear-locals test)]
      (assoc ast
        :test test
        :then then
        :else else))))

(defmethod -clear-locals :case
  [{:keys [test default thens] :as ast}]
  (let [[thens thens-locals]
        (reduce (fn [[thens locals] then]
                  (let [[t l] (binding [*clears* (atom @*clears*)]
                                [(-clear-locals then) (:locals @*clears*)])]
                    [(conj thens t) (into locals l)]))
                [[] #{}] thens)
        [default {:keys [locals]}] (binding [*clears* (atom @*clears*)]
                                     [(-clear-locals default) @*clears*])]
    (swap! *clears* #(update-in % [:locals] into (into thens-locals locals)))
    (assoc ast
      :test    test
      :thens   thens
      :default default)))

(defmethod -clear-locals :local
  [ast]
  (let [ast (maybe-clear-local ast)]
    (swap! *clears* #(update-in % [:locals] conj (:name ast))) ;; register that the local has been used and potentially cleared
    ast))

(defn clear-locals
  [ast]
  (binding [*clears* (atom {:closed-overs      {}
                            :loop-closed-overs {}
                            :locals            #{}})]
    (-clear-locals ast)))
