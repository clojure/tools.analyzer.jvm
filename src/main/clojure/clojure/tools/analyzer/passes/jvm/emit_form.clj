;:   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.emit-form
  (:require [clojure.tools.analyzer.passes.emit-form :refer [-emit-form]]))

(defn emit-form [ast]
  (-emit-form ast false))

(defn emit-hygienic-form [ast]
  (-emit-form ast true))

(defmethod -emit-form :monitor-enter
  [{:keys [target]} hygienic?]
  `(monitor-enter ~target))

(defmethod -emit-form :monitor-exit
  [{:keys [target]} hygienic?]
  `(monitor-exit ~target))

(defmethod -emit-form :import
  [{:keys [class]} hygienic?]
  `(clojure.core/import* ~(.getName ^Class class)))

(defmethod -emit-form :the-var
  [{:keys [^clojure.lang.Var var]} hygienic?]
  `(var ~(symbol (name (ns-name (.ns var))) (name (.sym var)))))

(defmethod -emit-form :method
  [{:keys [params body this name]} hygienic?]
  (let [params (into [this] params)]
    `(~name ~(mapv #(-emit-form % hygienic?) params)
            ~(-emit-form body hygienic?))))

(defmethod -emit-form :deftype
  [{:keys [name class-name fields interfaces methods]} hygienic?]
  `(deftype* ~name ~class-name ~(mapv #(-emit-form % hygienic?) fields)
     :implements ~(vec interfaces)
     ~@(mapv #(-emit-form % hygienic?) methods)))

(defmethod -emit-form :reify
  [{:keys [interfaces methods]} hygienic?]
  `(reify* ~(vec interfaces)
           ~@(mapv #(-emit-form % hygienic?) methods)))

(defmethod -emit-form :case
  [{:keys [test default tests thens shift mask low high switch-type test-type skip-check?]} hygienic?]
  `(case* ~(-emit-form test hygienic?)
          ~shift ~mask
          ~(-emit-form default hygienic?)
          ~(apply sorted-map
                  (mapcat (fn [{:keys [hash test]} {:keys [then]}]
                            [hash [(-emit-form test hygienic?) (-emit-form then hygienic?)]])
                          tests thens))
          ~switch-type ~test-type ~skip-check?))

(defmethod -emit-form :static-field
  [{:keys [class field]} hygienic?]
  (symbol (.getName ^Class class) (name field)))

(defmethod -emit-form :static-call
  [{:keys [class method args]} hygienic?]
  `(~(symbol (.getName ^Class class) (name method))
    ~@(mapv #(-emit-form % hygienic?) args)))

(defmethod -emit-form :instance-field
  [{:keys [instance field]} hygienic?]
  `(~(symbol (str ".-" (name field))) ~(-emit-form instance hygienic?)))

(defmethod -emit-form :instance-call
  [{:keys [instance method args]} hygienic?]
  `(~(symbol (str "." (name method))) ~(-emit-form instance hygienic?)
    ~@(mapv #(-emit-form % hygienic?) args)))

(defmethod -emit-form :host-interop
  [{:keys [target m-or-f]} hygienic?]
  `(~(symbol (str "." (name m-or-f))) ~(-emit-form target hygienic?)))
