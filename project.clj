(defproject org.clojure/tools.analyzer.jvm "0.7.3-SNAPSHOT"
  :description "Additional jvm-specific passes for tools.analyzer."
  :url "https://github.com/clojure/tools.analyzer.jvm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.memoize "1.1.266"]
                 [org.clojure/tools.reader "1.5.0"]
                 [org.clojure/tools.analyzer "1.2.0"]
                 [org.ow2.asm/asm "9.2"]]
  :repositories [["sonatype" "https://oss.sonatype.org/content/repositories/releases"]
                 ["snapshots" "https://oss.sonatype.org/content/repositories/snapshots"]])
