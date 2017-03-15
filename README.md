# tools.analyzer.jvm

An analyzer for Clojure code, written on top of [tools.analyzer](https://github.com/clojure/tools.analyzer), providing additional jvm-specific passes.

* [Example Usage](#example-usage)
* [AST Quickref](#ast-quickref)
* [Releases and Dependency Information](#releases-and-dependency-information)
* [Changelog](#changelog)
* [API Index](#api-index)
* [Developer Information](#developer-information)
* [License](#license)

## Note for REPL usage

The AST `tools.analyzer.jvm` produces contains *a lot* of redundant information and while having this structure in memory will not require an excessive amount of memory thanks to structural sharing, attempting to print the AST of even a relatively small clojure expression can easily produce a several thousand lines output which might make your REPL irresponsive for several seconds or even crash it.
For this reason, when exploring `tools.analyzer.jvm` ASTs on the REPL, I encourage you to:
* set `*print-length*` and `*print-level*` to a small value, like 10
* interactively explore the AST structure, inspecting the `:children` and `:op` fields of a node and the `keys` function rather than printing it to see its content

## Example Usage

Calling `analyze` on the form is all it takes to get its AST (the output has been pretty printed for clarity):
```clojure
user> (require '[clojure.tools.analyzer.jvm :as ana.jvm])
nil
user> (ana.jvm/analyze 1)
{:op        :const,
 :env       {:context :ctx/expr, :locals {}, :ns user},
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

There's also an `analyze+eval` function that, as the name suggests, evaluates the form after its analysis and stores the resulting value in the `:result` field of the AST, this function should be used when analyzing multiple forms, as the analysis of a clojure form might require the evaluation of a previous one to make sense.

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
 :env       {:context :ctx/expr, :locals {}, :ns user},
 :form      nil,
 :top-level true,
 :val       nil,
 :type      :nil,
 :literal?  true,
 :tag       java.lang.Object,
 :o-tag     java.lang.Object,
 :result    nil}
```

To analyze a whole namespace, use `analyze-ns` which behaves like `analyze+eval` and puts the ASTs for each analyzed form in a vector, in order.
```clojure
user> (ana.jvm/analyze-ns 'clojure.string)
[{:op        :do,
  :result    nil,
  :top-level true,
  :form      (do (clojure.core/in-ns (quote clojure.string)) ..),
  ...}
..]
```

[AST Quickref](http://clojure.github.io/tools.analyzer.jvm/spec/quickref.html)
========================================
Note that the quickref refers to the last stable release of t.a.jvm and might not be valid for the current SNAPSHOT version or for previous ones.
Note also that the documented node fields refer to the output of t.a.jvm/analyze running the default passes and using the default configuration.

## SPONSORSHIP

* Cognitect (http://cognitect.com/) has sponsored tools.analyzer.jvm development (https://groups.google.com/d/msg/clojure/iaP16MHpX0E/EMtnGmOz-rgJ)
* Ambrose BS (https://twitter.com/ambrosebs) has sponsored tools.analyzer.jvm development in his typed clojure campaign (http://www.indiegogo.com/projects/typed-clojure).

## YourKit

YourKit has given an open source license for their profiler, greatly simplifying the profiling of tools.analyzer.jvm performance.

YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products:

* <a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
* <a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.

Releases and Dependency Information
========================================

Latest stable release: 0.7.0

* [All Released Versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.analyzer.jvm%22)

* [Development Snapshot Versions](https://oss.sonatype.org/index.html#nexus-search;gav%7Eorg.clojure%7Etools.analyzer.jvm%7E%7E%7E)

[Leiningen](https://github.com/technomancy/leiningen) dependency information:

```clojure
[org.clojure/tools.analyzer.jvm "0.7.0"]
```
[Maven](http://maven.apache.org/) dependency information:

```xml
<dependency>
  <groupId>org.clojure</groupId>
  <artifactId>tools.analyzer.jvm</artifactId>
  <version>0.7.0</version>
</dependency>
```

[Changelog](CHANGELOG.md)
========================================

API Index
========================================

* [CrossClj Documentation](http://crossclj.info/doc/org.clojure/tools.analyzer.jvm/lastest/index.html)
* [API index](http://clojure.github.io/tools.analyzer.jvm)

Developer Information
========================================

* [GitHub project](https://github.com/clojure/tools.analyzer.jvm)

* [Bug Tracker](http://dev.clojure.org/jira/browse/TANAL)

* [Continuous Integration](http://build.clojure.org/job/tools.analyzer.jvm/)

* [Compatibility Test Matrix](http://build.clojure.org/job/tools.analyzer.jvm-test-matrix/)

## License

Copyright © 2013-2017 Nicola Mometto, Rich Hickey & contributors.

Distributed under the Eclipse Public License, the same as Clojure.
