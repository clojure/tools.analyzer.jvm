;:   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.analyze-host-expr
  (:require [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.utils :refer [ctx]]
            [clojure.tools.analyzer.jvm.utils :refer :all]))

(defn maybe-static-field [[_ class sym]]
  (when-let [{:keys [flags type]} (static-field class sym)]
    {:op          :static-field
     :assignable? (not (:final flags))
     :class       class
     :field       sym
     :ret-tag     (maybe-class type)
     :tag         (maybe-class type)}))

(defn maybe-static-method [[_ class sym]]
  (when-let [{:keys [return-type]} (static-method class sym)]
    {:op      :static-call
     :tag     (maybe-class return-type)
     :ret-tag (maybe-class return-type)
     :class   class
     :method  sym}))

(defn maybe-instance-method [target-expr class sym]
  (when-let [{:keys [return-type]} (instance-method class sym)]
      {:op       :instance-call
       :tag      (maybe-class return-type)
       :ret-tag  (maybe-class return-type)
       :instance target-expr
       :class    class
       :method   sym
       :children [:instance]}))

(defn maybe-instance-field [target-expr class sym]
  (when-let [{:keys [flags type]} (instance-field class sym)]
    {:op          :instance-field
     :assignable? (not (:final flags))
     :class       class
     :instance    target-expr
     :field       sym
     :tag         (maybe-class type)
     :ret-tag     (maybe-class type)
     :children    [:instance]}))

(defn analyze-host-call
  [target-type method args target-expr class env]
  (let [op (case target-type
             :static   :static-call
             :instance :instance-call)]
    (merge
     {:op     op
      :method method
      :args   args}
     (case target-type
       :static   {:class    class
                  :children [:args]}
       :instance {:instance target-expr
                  :class    (maybe-class (:tag target-expr))
                  :children [:instance :args]}))))

(defn analyze-host-field
  [target-type field target-expr class env]
  (if class
    (case target-type
      :static (or (maybe-static-field (list '. class field))
                  (throw (ex-info (str "cannot find field "
                                       field " for class " class)
                                  {:class class
                                   :field field})))
      :instance (or (maybe-instance-field target-expr class field)
                    (throw (ex-info (str "cannot find field "
                                         field " for class " class)
                                    {:instance target-expr
                                     :field    field}))))
    {:op       :host-interop
     :target   target-expr
     :m-or-f   field
     :children [:target]}))

(defn -analyze-host-expr
  [target-type m-or-f target-expr class env]
  (if class
    (or (maybe-static-field (list '. class m-or-f))
        (maybe-static-method (list '. class m-or-f))
        (throw (ex-info (str "cannot find field or no-arg method call "
                             m-or-f " for class " class)
                        {:class  class
                         :m-or-f m-or-f})))
    (if-let [class (maybe-class (-> target-expr :tag))]
      (or (maybe-instance-field target-expr class m-or-f)
          (maybe-instance-method target-expr class m-or-f)
          (throw (ex-info (str "cannot find field or no-arg method call "
                               m-or-f " for class " class)
                          {:instance target-expr
                           :m-or-f   m-or-f})))
      {:op          :host-interop
       :target      target-expr
       :m-or-f      m-or-f
       :assignable? true
       :children    [:target]})))

(defn analyze-host-expr
  [{:keys [op form tag env] :as ast}]
  (if (#{:host-interop :host-call :host-field} op)
    (let [target (:target ast)
          class? (and (= :const (:op target))
                      (maybe-class (:form target)))
          target-type (if class? :static :instance)]
      (merge {:form form
              :env  env}
             (case op

               :host-call
               (analyze-host-call target-type (:method ast)
                                  (:args ast) target class? env)

               :host-field
               (analyze-host-field target-type (:field ast)
                                   target (or class?
                                              (maybe-class (:tag target))) env)

               (-analyze-host-expr target-type (:m-or-f ast)
                                   target class? env))
             (when tag
               {:tag tag})))
    ast))
