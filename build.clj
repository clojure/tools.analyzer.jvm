(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def basis
  (b/create-basis {:project "deps.edn"}))

(defn compile-test-java [_]
  (b/javac {:src-dirs ["src/test/java"]
            :class-dir "target/test-classes"
            :basis basis}))
