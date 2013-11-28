{:namespaces
 ({:source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm/clojure.tools.analyzer.jvm-api.html",
   :name "clojure.tools.analyzer.jvm",
   :doc
   "Analyzer for clojure code, extends tools.analyzer with JVM specific passes/forms"}),
 :vars
 ({:arglists ([form env]),
   :name "analyze",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj#L296",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze",
   :doc
   "Returns an AST for the form that's compatible with what tools.emitter.jvm requires.\n\nBinds tools.analyzer/{macroexpand-1,create-var,parse} to\ntools.analyzer.jvm/{macroexpand-1,create-var,parse} and calls\ntools.analyzer/analyzer on form.\n\nApplies the following passes in the correct order to the returning AST:\n* uniquify\n* add-binding-atom\n* cleanup\n* source-info\n* elide-meta\n* constant-lifter\n* warn-earmuff\n* collect\n* jvm.box\n* jvm.annotate-branch\n* jvm.annotate-methods\n* jvm.fix-case-test\n* jvm.clear-locals\n* jvm.classify-invoke\n* jvm.validate\n* jvm.infer-tag\n* jvm.annotate-tag\n* jvm.validate-loop-locals\n* jvm.analyze-host-expr",
   :var-type "function",
   :line 296,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([sym {:keys [ns]}]),
   :name "create-var",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj#L138",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/create-var",
   :doc
   "Creates a Var for sym and returns it.\nThe Var gets interned in the env namespace.",
   :var-type "function",
   :line 138,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([]),
   :name "empty-env",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj#L52",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/empty-env",
   :doc "Returns an empty env map",
   :var-type "function",
   :line 52,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form env]),
   :name "macroexpand-1",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj#L108",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/macroexpand-1",
   :doc
   "If form represents a macro form or an inlineable function,\nreturns its expansion, else returns form.",
   :var-type "function",
   :line 108,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj#L44",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/parse",
   :namespace "clojure.tools.analyzer.jvm",
   :line 44,
   :var-type "multimethod",
   :doc "Extension to tools.analyzer/-parse for JVM special forms",
   :name "parse"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/7be5a20bcfaadddbee95ea0366d62575eb6f04c8/src/main/clojure/clojure/tools/analyzer/jvm.clj#L39",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/specials",
   :namespace "clojure.tools.analyzer.jvm",
   :line 39,
   :var-type "var",
   :doc "Set of the special forms for clojure in the JVM",
   :name "specials"})}
