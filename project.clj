(defproject org.clojure/tools.analyzer.jvm "0.6.10-SNAPSHOT"
  :description "Additional jvm-specific passes for tools.analyzer."
  :url "https://github.com/clojure/tools.analyzer.jvm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :dependencies [[org.clojure/clojure "1.9.0-master-SNAPSHOT"]
                 [org.clojure/core.memoize "0.5.9"]
                 [org.clojure/tools.reader "1.0.0-beta2"]
                 [org.clojure/tools.analyzer "0.6.9"]
                 [org.ow2.asm/asm-all "4.2"]])
