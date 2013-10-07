;:   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.analyzer.passes.jvm.annotate-methods
  (:require [clojure.tools.analyzer.jvm.utils :refer [type-reflect]]))

(defn annotate-methods
  [{:keys [op methods interfaces] :as ast}]
  (case op
    (:reify :deftype)
    (let [all-methods
          (into #{}
                (mapcat (fn [class]
                          (mapv (fn [method]
                                  (dissoc method :exception-types))
                                (remove (fn [{:keys [flags return-type]}]
                                          (or (some #{:static :final} flags)
                                              (not-any? #{:public :protected} flags)
                                              (not return-type)))
                                        (:members (type-reflect class :ancestors true)))))
                        (conj interfaces Object)))]
      (assoc ast :methods (mapv (fn [{:keys [name params] :as ast}]
                                  (let [argc (count params)]
                                   (assoc ast :methods
                                          (filter #(and (= name (:name %))
                                                        (= argc (count (:parameter-types %))))
                                                  all-methods)))) methods)))
    ast))
