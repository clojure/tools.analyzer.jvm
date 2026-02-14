(ns build
  (:require [clojure.tools.build.api :as b]))

(defn compile-test-java [_]
  (b/javac {:src-dirs ["src/test/java"]
            :class-dir "target/test-classes"}))
