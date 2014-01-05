(ns clojure.tools.analyzer.passes.jvm.type-coerce
  (:require [clojure.tools.analyzer.jvm.utils :refer [primitive? numeric? prim-or-obj]]))

(defn emit-box [{:keys [tag env form] :as ast} ret]
  (let [box tag
        tag ret]
   (if (and (primitive? tag)
            (not (primitive? box)))
     (cond
      (numeric? tag)
      {:op         :static-call
       :env        env
       :class      clojure.lang.RT
       :method     'box
       :cast       box
       :tag        box
       :args       [ast]
       :validated? true
       :form       form
       :ret-tag    java.lang.Number
       :children [:args]}

      (= Character/TYPE tag)
      {:op         :static-call
       :env        env
       :class      clojure.lang.RT
       :method     'box
       :args       [ast]
       :tag        java.lang.Character
       :validated? true
       :form       form
       :ret-tag    java.lang.Character
       :children [:args]}

      (= Boolean/TYPE tag)
      {:op         :static-call
       :env        env
       :class      clojure.lang.RT
       :method     'box
       :cast       java.lang.Boolean
       :tag        java.lang.Boolean
       :args       [ast]
       :validated? true
       :form       form
       :ret-tag    java.lang.Object
       :children [:args]})

     (when (primitive? box)
       (let [method (symbol (str (.getName ^Class box) "Cast"))
             tag (prim-or-obj tag)]
         {:op         :static-call
          :env        env
          :class      clojure.lang.RT
          :method     method
          :tag        box
          :args       [ast]
          :validated? true
          :form       form
          :ret-tag    box
          :children [:args]})))))

(defn emit-cast [{:keys [tag] :as ast} cast]
  (if (not (or (primitive? tag)
             (primitive? cast)))
    (when-not (#{Void Void/TYPE} cast)
      (assoc ast :cast cast))
    (emit-box ast cast)))

(defmulti -type-coerce :op)
(defmethod -type-coerce :default [ast] ast)

(defmethod -type-coerce :def
  [{:keys [meta] :as ast}]
  (if meta
    (assoc-in ast [:meta :tag] clojure.lang.IPersistentMap)
    ast))

(defmethod -type-coerce :throw
  [ast]
  (assoc-in ast [:exception :tag] java.lang.Throwable))

(defmethod -type-coerce :with-meta
  [ast]
  (-> ast
    (assoc-in [:expr :tag] clojure.lang.IObj)
    (assoc-in [:meta :tag] clojure.lang.IPersistentMap)))

(defmethod -type-coerce :instance-field
  [{:keys [class] :as ast}]
  (assoc-in ast [:instance :tag] class))

(defmethod -type-coerce :instance-call
  [{:keys [class] :as ast}]
  (assoc-in ast [:instance :tag] class))

(defmethod -type-coerce :method
  [{:keys [tag body] :as ast}]
  (-> ast
    (assoc-in [:body :tag] tag)
    (assoc-in [:body :ret-tag] (or (:tag body) Object))))

(defmethod -type-coerce :set!
  [{:keys [target val] :as ast}]
  (let [t-op (:op target)]
    (case t-op
      :static-field
      (assoc-in ast [:val :tag] (:ret-tag target))
      :instance-field
      (-> ast
        (assoc-in [:val :tag] (:ret-tag target))
        (assoc-in [:instance :tag] (:class target)))
      ast)))

(defn type-coerce [{:keys [ret-tag tag bind-tag] :as ast}]
  (let [ast  (-type-coerce ast)
        tag' (or ret-tag bind-tag)]
    (if (and tag tag'
             (not= tag tag'))
      (emit-cast ast ret-tag)
      ast)))
