;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.process-method-value
  (:require [clojure.tools.analyzer.utils :refer [source-info]]
            [clojure.tools.analyzer.passes.jvm.analyze-host-expr :refer [analyze-host-expr]]))

(defn process-method-value
  "Transforms :invoke nodes whose :fn is a :method-value into the
   corresponding :instance-call, :static-call, or :new node. "
  {:pass-info {:walk :post :depends #{#'analyze-host-expr}}}
  [{:keys [op] :as ast}]
  (if (and (= :invoke op)
           (= :method-value (:op (:fn ast))))
    (let [{:keys [args env]} ast
          {:keys [class method kind param-tags]} (:fn ast)]
      (when (and (= :instance kind) (empty? args))
        (throw (ex-info (str "Qualified instance method " (.getName ^Class class) "/." method
                             " must have a target")
                        (merge {:class class :method method}
                               (source-info env)))))
      (merge (dissoc ast :fn :args)
             (case kind
               :instance
               {:op       :instance-call
                :method   method
                :class    class
                :instance (first args)
                :args     (vec (rest args))
                :children [:instance :args]}

               :static
               {:op       :static-call
                :method   method
                :class    class
                :args     args
                :children [:args]}

               :ctor
               {:op       :new
                :class    {:op :const :type :class :val class
                           :form class :env env}
                :args     args
                :children [:class :args]})
             (when param-tags
               {:param-tags param-tags})))
    ast))
