;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.clear-locals
  (:require [clojure.tools.analyzer.ast :refer [walk]]))

(def ^:dynamic *clears*)

(defn -clear-locals
  [{:keys [op name local path? should-not-clear env] :as ast}]
  (let [{:keys [closes clears]} @*clears*]
   (cond
    (and (= :local op)
         (#{:let :loop :letfn :arg} local)
         (or (not (closes name))
             (:once env))
         (not (clears name))
         (not should-not-clear))
    (do
      (swap! *clears* update-in [:branch-clears] conj name)
      (swap! *clears* update-in [:clears] conj name)
      (assoc ast :to-clear? true))

    (and (#{:invoke :static-call :instance-call} op)
         (= :return (:context env))
         (not (:in-try env)))
    (assoc ast :to-clear? true)

    :else
    ast)))

(defn clear-locals-around
  [{:keys [path? branch?] :as ast}]
  (let [ast (-clear-locals ast)]
    (when path?
      (let [{:keys [top-clears clears branch-clears]} @*clears*]
        (when branch?
          (swap! *clears* assoc :branch-clears top-clears)
          (swap! *clears* assoc :top-clears #{}))
        (doseq [c (:branch-clears @*clears*)]
          (when (clears c)
            (swap! *clears* update-in [:clears] disj c)))))
    ast))

(defn -propagate-closed-overs
  [{:keys [op test? path? closed-overs] :as ast}]
  (when (#{:reify :fn :deftype} op)
    (swap! *clears* assoc-in [:closes] (or closed-overs #{})))
  (when test?
    (swap! *clears* update-in [:clears] into (:branch-clears @*clears*))
    (swap! *clears* assoc :top-clears (:branch-clears @*clears*))
    (swap! *clears* assoc :branch-clears #{}))
  ast)

(defn clear-locals
  "Walks the AST and injects :to-clear? to local nodes in a position suitable for
   their clearing (this means that they are in the last reachable position for the
   branch they are in)"
  [ast]
  (binding [*clears* (atom {:branch-clears #{}
                            :top-clears    #{}
                            :clears        #{}
                            :closes        #{}})]
    (walk ast -propagate-closed-overs clear-locals-around :reversed)))
