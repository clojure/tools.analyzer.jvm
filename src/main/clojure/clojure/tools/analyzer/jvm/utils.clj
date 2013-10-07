;:   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.jvm.utils
  (:require [clojure.reflect :as reflect])
  (:import (clojure.lang RT Symbol Var)
           (org.objectweb.asm Type)))

(defn type-reflect [typeref & options]
  (apply reflect/type-reflect typeref
         :reflector (reflect/->JavaReflector (RT/baseLoader))
         options))

(def ^:private specials
  {"byte" Byte/TYPE
   "boolean" Boolean/TYPE
   "char" Character/TYPE
   "int" Integer/TYPE
   "long" Long/TYPE
   "float" Float/TYPE
   "double" Double/TYPE
   "short" Short/TYPE
   "void" Void/TYPE
   "object" Object})

(defmulti ^Class maybe-class class)

(defn array-class [element-type]
  (RT/classForName
   (str "[" (-> element-type
              maybe-class
              Type/getType
              .getDescriptor
              (.replace \/ \.)))))

(defn maybe-class-from-string [s]
  (try
    (RT/classForName s)
    (catch Exception _
      (if-let [maybe-class ((ns-map *ns*) (symbol s))]
        (when (class? maybe-class)
          maybe-class)))))

(defmethod maybe-class :default [_] nil)
(defmethod maybe-class Class [c] c)
(defmethod maybe-class String [s]
  (maybe-class (symbol s)))

(defmethod maybe-class Symbol [sym]
  (when-not (namespace sym)
    (let [sname (name sym)
          snamec (count sname)]
      (if-let [base-type (or (and (.endsWith sname "<>")
                                  (maybe-class (subs sname 0 (- snamec 2))))
                             (and (.endsWith sname "s")
                                  (maybe-class (subs sname 0 (dec snamec)))))]
        (array-class base-type)
        (if-let [ret (specials sname)]
          ret
          (maybe-class-from-string sname))))))

(defn primitive? [o]
  (let [c (maybe-class o)]
    (and
     (not (or (nil? c) (= c Void/TYPE)))
     (.isPrimitive c))))

(def convertible-primitives?
  {Integer/TYPE   #{Integer Long/TYPE Long Short/TYPE Byte/TYPE}
   Float/TYPE     #{Float Double/TYPE}
   Double/TYPE    #{Double Float/TYPE}
   Long/TYPE      #{Long Integer/TYPE Short/TYPE Byte/TYPE}
   Character/TYPE #{Character}
   Short/TYPE     #{Short}
   Byte/TYPE      #{Byte}
   Boolean/TYPE   #{Boolean}
   Void/TYPE      #{Void}})

(defn ^Class box [c]
  ({Integer/TYPE   Integer
    Float/TYPE     Float
    Double/TYPE    Double
    Long/TYPE      Long
    Character/TYPE Character
    Short/TYPE     Short
    Byte/TYPE      Byte
    Boolean/TYPE   Boolean
    Void/TYPE      Void}
   c c))

(defn ^Class unbox [c]
  ({Integer   Integer/TYPE,
    Long      Long/TYPE,
    Float     Float/TYPE,
    Short     Short/TYPE,
    Boolean   Boolean/TYPE,
    Byte      Byte/TYPE,
    Character Character/TYPE,
    Double    Double/TYPE,
    Void      Void/TYPE}
   c c))

(defn numeric? [c]
  (.isAssignableFrom Number (box c)))

(defn subsumes? [c1 c2]
  (let [c1 (maybe-class c1)
        c2 (maybe-class c2)]
    (and (not= c1 c2)
         (or (and (not (primitive? c1))
                  (primitive? c2))
             (.isAssignableFrom c2 c1)))))

(defn convertible? [from to]
  (let [c1 (maybe-class from)
        c2 (maybe-class to)]
    (if (nil? c1)
      (not (primitive? c2))
      (or
       (= c1 c2)
       (.isAssignableFrom c2 c1)
       (and (primitive? c2)
            ((convertible-primitives? c2) c1))))))

(defn members [class member]
  (let [members (-> (maybe-class class)
                  box
                  (type-reflect :ancestors true)
                  :members)]
    (when-let [members (filter #(= member (:name %)) members)]
      members)))

(defn static-members [class f]
  (when-let [members (members class f)]
    (when-let [statics (filter (comp :static :flags) members)]
      statics)))

(defn instance-members [class f]
  (when-let [members (members class f)]
    (when-let [i-members (remove (comp :static :flags) members)]
      i-members)))

(defn static-methods [class method argc]
  (filter #(= argc (count (:parameter-types %)))
          (filter :return-type (static-members class method))))

(defn instance-methods [class method argc]
  (filter #(= argc (count (:parameter-types %)))
          (filter :return-type (instance-members class method))))

(defn static-field [class f]
  (when-let [statics (static-members class f)]
    (when-let [[member] (filter (every-pred (comp nil? seq :parameter-types)
                                            (comp nil? :return-type))
                                statics)]
      member)))

(defn instance-field [class f]
  (when-let [i-members (instance-members class f)]
    (when-let [[member] (filter (every-pred (comp nil? seq :parameter-types)
                                            (comp nil? :return-type))
                                i-members)]
      member)))

(defn static-method [class method]
  (first (static-methods class method 0)))

(defn instance-method [class method]
  (first (instance-methods class method 0)))

(defn prim-or-obj [tag]
  (if (and tag (primitive? tag))
    tag
    java.lang.Object))

(defn prim-interface [tags]
  (when (some primitive? tags)
    (let [sig (apply str (mapv #(.toUpperCase (subs (.getName ^Class %) 0 1)) tags))]
      (maybe-class (str "clojure.lang.IFn$" sig)))))
