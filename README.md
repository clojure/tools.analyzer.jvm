# tools.analyzer.jvm

Additional jvm-specific passes for tools.analyzer

## Example Usage

Calling `analyze` on the form is all it takes to get its AST (the output has been pretty-printed for clarity):
```clojure
user> (require '[clojure.tools.analyzer.jvm :as ana.jvm])
nil
user> (ana.jvm/analyze 1)
{:op        :const,
 :env       {:context :expr, :locals {}, :ns user},
 :form      1,
 :top-level true,
 :val       1,
 :type      :number,
 :literal?  true,
 :id        0,
 :tag       long,
 :o-tag     long}
```

To get a clojure form out of an AST, use the `emit-form` pass:
```clojure
user> (require '[clojure.tools.analyzer.passes.jvm.emit-form :as e])
nil
user> (e/emit-form (ana.jvm/analyze '(let [a 1] a)))
(let* [a 1] a)
```
Note that the output will be fully macroexpanded.
You can also get an hygienic form back, using the `emit-hygienic-form` pass:
```clojure
user> (e/emit-hygienic-form (ana.jvm/analyze '(let [a 1 a a] a)))
(let* [a__#0 1 a__#1 a__#0] a__#1)
```
As you can see the local names are renamed to resolve shadowing.

The `analyze` function can take an environment arg (when not provided it uses the default empty-env) which allows for more advanced usages, like injecting locals from an outer scope:
```clojure
user> (-> '(let [a a] a)
        (ana.jvm/analyze (assoc (ana.jvm/empty-env)
                           :locals '{a {:op    :binding
                                        :name  a
                                        :form  a
                                        :local :let}}))
        e/emit-hygienic-form)
(let* [a__#0 a] a__#0)
```

When using `tools.analyzer.jvm` for analyzing whole namespaces or whole files, you should use `analyze+eval` rather than `analyze`; as the name suggests, `analyze+eval` evals the form after its analysis and stores the resulting value in the `:result` field of the AST.

This would not work using `analyze` but works fine when using `analyze+eval`:
```clojure
user> (ana.jvm/analyze+eval '(defmacro x []))
{:op        :do,
 :top-level true,
 :form      (do (clojure.core/defn x ([&form &env])) (. (var x) (setMacro)) (var x)),
 ... ,
 :result    #'user/x}
user> (ana.jvm/analyze+eval '(x))
{:op        :const,
 :env       {:context :expr, :locals {}, :ns user},
 :form      nil,
 :top-level true,
 :val       nil,
 :type      :nil,
 :literal?  true,
 :tag       java.lang.Object,
 :o-tag     java.lang.Object,
 :result    nil}
```

### A note about environments

Until version 0.1.0-beta13 it was required to provide an environment to analyze/analyze+eval and maintain/share said environment across analysis when using `tools.analyzer.jvm` to analyze whole namespaces.
Since version 0.2.0 this is no longer required nor encouraged and an explicit environment should be provided only when strictly necessary (see the example above with the constructed locals for a good use-case)

Version 0.2.0 introduced the notion of a global environment which now holds the namespaces info rather then the analysis environment.

When analyzing whole namespaces/files, it is *strongly encouraged* to provide a global environment shared across analysis, by wrapping the analysis loop in a `ana/with-env`, here's a proof of concept:
```clojure
user> (require '[clojure.tools.analyzer.env :as env])
nil
user> (env/with-env (ana.jvm/global-env)
        (loop [forms []]
          (let [form (read stream nil sentinel)]
            (if (= sentinel form)
              forms
              (recur (conj forms (ana.jvm/analyze+eval form)))))))
```
This is not required but it's encouraged for the sake of unifying behaviour between `tools.analyzer.jvm` and `tools.analyzer.js` and to potentially allow some passes to share info across analysis.

## SPONSORSHIP

* Cognitect (http://cognitect.com/) is sponsoring tools.analyzer.jvm development (https://groups.google.com/d/msg/clojure/iaP16MHpX0E/EMtnGmOz-rgJ)
* Ambrose BS (https://twitter.com/ambrosebs) has sponsored tools.analyzer.jvm development in his typed clojure campaign (http://www.indiegogo.com/projects/typed-clojure).

## YourKit

YourKit has given an open source license for their profiler, greatly simplifying the profiling of tools.analyzer.jvm performance.

YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products:

* <a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
* <a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.

Releases and Dependency Information
========================================

Latest stable release: 0.2.2

* [All Released Versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.analyzer.jvm%22)

* [Development Snapshot Versions](https://oss.sonatype.org/index.html#nexus-search;gav%7Eorg.clojure%7Etools.analyzer.jvm%7E%7E%7E)

[Leiningen](https://github.com/technomancy/leiningen) dependency information:

```clojure
[org.clojure/tools.analyzer.jvm "0.2.2"]
```
[Maven](http://maven.apache.org/) dependency information:

```xml
<dependency>
  <groupId>org.clojure</groupId>
  <artifactId>tools.analyzer.jvm</artifactId>
  <version>0.2.2</version>
</dependency>
```

[Changelog](CHANGELOG.md)
========================================

Developer Information
========================================

* [GitHub project](https://github.com/clojure/tools.analyzer.jvm)

* [Bug Tracker](http://dev.clojure.org/jira/browse/TANAL)

* [Continuous Integration](http://build.clojure.org/job/tools.analyzer.jvm/)

* [Compatibility Test Matrix](http://build.clojure.org/job/tools.analyzer.jvm-test-matrix/)

## License

Copyright © 2013-2014 Nicola Mometto, Rich Hickey & contributors.

Distributed under the Eclipse Public License, the same as Clojure.
