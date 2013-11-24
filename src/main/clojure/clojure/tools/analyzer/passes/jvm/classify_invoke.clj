;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.classify-invoke
  (:require [clojure.tools.analyzer.utils :refer [arglist-for-arity protocol-node?]]
            [clojure.tools.analyzer.jvm.utils
             :refer [maybe-class prim-or-obj primitive? prim-interface]]))

(defn classify-invoke
  "If the AST node is an :invoke, check the node in function position,
   * if it is a keyword, transform the node in a :keyword-invoke node;
   * if it is the clojure.core/instance? var and the first argument is a
     literal class, transform the node in a :instance? node to be inlined by
     the emitter
   * if it is a protocol function var, transform the node in a :protocol-invoke
     node
   * if it is a regular function with primitive type hints that match a
     clojure.lang.IFn$[primitive interface], transform the node in a :prim-invoke
     node"
  [{:keys [op args tag env fn form] :as ast}]
  (if-not (= op :invoke)
    ast
    (let [argc (count args)
          op (:op fn)
          var? (= :var op)
          the-var (:var fn)
          arglist (arglist-for-arity fn argc)
          arg-tags (mapv (comp prim-or-obj maybe-class :tag meta) arglist)
          ret-tag (prim-or-obj (maybe-class (:tag (meta arglist))))
          prim-interface (prim-interface (conj arg-tags ret-tag))]

      (cond

       (and (= :const op)
            (= :keyword (:type fn)))
       (if (<= 1 argc 2)
         (assoc ast :op :keyword-invoke)
         (throw (ex-info (str "Cannot invoke keyword with " argc " arguments")
                         {:form form})))
       (and (= 2 argc)
            var?
            (= #'clojure.core/instance? the-var)
            (= :const (:op (first args)))
            (= :class (:type (first args))))
       {:op       :instance?
        :class    (:form (first args))
        :target   (second args)
        :form     form
        :env      env
        :ret-tag  Boolean/TYPE
        :tag      (or tag Boolean/TYPE)
        :children [:target]}

       (and var? (protocol-node? the-var))
       (if (>= argc 1)
         (assoc ast :op :protocol-invoke)
         (throw (ex-info "Cannot invoke protocol method with no args"
                         {:form form})))

       prim-interface
       (assoc ast
         :op :prim-invoke
         :prim-interface prim-interface
         :arg-tags arg-tags
         :ret-tag ret-tag
         :tag (or tag ret-tag))

       :else
       ast))))
