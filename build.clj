(ns build
  "Build script"
  (:require 
   [clojure.tools.build.api :as b]
   [org.corfield.build :as bb]))

(def project-opts
  {:lib 'robertluo/tools.analyzer.jvm
   :version (format "0.8.%s" (b/git-count-revs nil))})

(defn project [opts]
  (merge project-opts opts))

(defn ci [opts]
  (-> opts project bb/run-tests bb/clean bb/jar))