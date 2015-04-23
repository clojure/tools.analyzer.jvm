(defproject org.clojure/tools.analyzer.jvm "0.6.8-SNAPSHOT"
  :description "Additional jvm-specific passes for tools.analyzer."
  :url "https://github.com/clojure/tools.analyzer.jvm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/tools.reader "0.9.2"]
                 [org.clojure/tools.analyzer "0.6.7-SNAPSHOT"]
                 [org.ow2.asm/asm-all "4.2"]])
