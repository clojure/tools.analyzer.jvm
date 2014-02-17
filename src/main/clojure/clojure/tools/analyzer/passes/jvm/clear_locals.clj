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

(defmethod -clear-locals :default
  [{:keys [closed-overs op once] :as ast}]
  (if closed-overs
    (let [[ast clears] (binding [*clears* (atom (update-in @*clears* [(if (= :loop op)
                                                                        :loop-closed-overs
                                                                        :closed-overs)]
                                                           merge closed-overs))]
                         [(update-children ast -clear-locals rseqv) @*clears*])
          locals (:locals @*clears*)
          [ks vs] (reduce-kv (fn [[keys vals] k v]
                               [(conj keys k) (conj vals v)])
                             [[] []] closed-overs)
          ast (if (and (= :fn op) once)
                (assoc ast :closed-overs (zipmap ks
                                                 (mapv (fn [{:keys [name] :as ast}]
                                                         (if (locals name)
                                                           ast
                                                           (assoc ast :to-clear? true)))
                                                       vs)))
                ast)]
      (swap! *clears* #(update-in % [:locals] into (:locals clears)))
      ast)
    (update-children ast -clear-locals rseqv)))

(defmethod -clear-locals :if
  [{:keys [test then else] :as ast}]
  (let [[then then-clears] (binding [*clears* (atom @*clears*)]
                             [(-clear-locals then) @*clears*])
        [else else-clears] (binding [*clears* (atom @*clears*)]
                             [(-clear-locals else) @*clears*])
        locals             (into (:locals then-clears)
                                 (:locals else-clears))]
    (swap! *clears* #(update-in % [:locals] into locals))
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
  [{:keys [name local should-not-clear env times] :as ast}]
  (let [{:keys [closed-overs locals loop-closed-overs]} @*clears*]
    (swap! *clears* #(update-in % [:locals] conj name))
    (if (and (#{:let :loop :letfn :arg} local)
             (or (not (loop-closed-overs name))
                 (not= :many times))
             (or (not (closed-overs name))
                 (:once env))
             (not (locals name))
             (not should-not-clear))
      (assoc ast :to-clear? true)
      ast)))

(defn clear-locals
  [ast]
  (binding [*clears* (atom {:closed-overs      {}
                            :loop-closed-overs {}
                            :locals            #{}})]
    (-clear-locals ast)))
