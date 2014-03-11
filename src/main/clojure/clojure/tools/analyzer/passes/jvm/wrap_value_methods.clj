;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.wrap-value-methods
  (:require [clojure.tools.analyzer :refer [analyze]]
            [clojure.tools.analyzer.jvm.utils :refer :all]))

(defmulti wrap-value-methods :op)

(defmethod wrap-value-methods :maybe-class
  [{:keys [class env] :as ast}]
  (if (.startsWith (str class) ".")
    (let [args (repeatedly 20 gensym)]
      (analyze `(fn* ~@(for [i (range 1 20) :let [args (vec (take i args))]]
                         (list args (list* class args)))) env))
    ast))

(defmethod wrap-value-methods :host-field
  [{:keys [target field env] :as ast}]
  (let [target (:form target)]
    (if-let [methods (and (maybe-class target)
                          (not (static-field target field))
                          (seq (filter :return-type (static-members target field))))]
      (let [argcs (filter (fn [x] (< x 20))
                          (distinct (map (comp count :parameter-types) methods)))
            args (repeatedly (apply max argcs) gensym)]
       (analyze `(fn* ~@(for [i argcs :let [args (vec (take i args))]]
                            (list args (list* (symbol (.getName ^Class target) (str field)) args)))) env))
      ast)))

(defmethod wrap-value-methods :default [ast] ast)
