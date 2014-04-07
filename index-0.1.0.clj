{:namespaces
 ({:source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm/clojure.tools.analyzer.jvm-api.html",
   :name "clojure.tools.analyzer.jvm",
   :doc
   "Analyzer for clojure code, extends tools.analyzer with JVM specific passes/forms"}
  {:source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm/clojure.tools.analyzer.jvm.utils-api.html",
   :name "clojure.tools.analyzer.jvm.utils",
   :doc nil}),
 :vars
 ({:arglists ([form env]),
   :name "analyze",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L449",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze",
   :doc
   "Returns an AST for the form that's compatible with what tools.emitter.jvm requires.\n\nBinds tools.analyzer/{macroexpand-1,create-var,parse} to\ntools.analyzer.jvm/{macroexpand-1,create-var,parse} and calls\ntools.analyzer/analyzer on form.\n\nCalls `run-passes` on the AST.",
   :var-type "function",
   :line 449,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form env]),
   :name "analyze+eval",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L465",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze+eval",
   :doc
   "Like analyze but evals the form after the analysis.\nUseful when analyzing whole files/namespaces.",
   :var-type "function",
   :line 465,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([sym {:keys [ns]}]),
   :name "create-var",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L173",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/create-var",
   :doc
   "Creates a Var for sym and returns it.\nThe Var gets interned in the env namespace.",
   :var-type "function",
   :line 173,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([]),
   :name "empty-env",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L79",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/empty-env",
   :doc "Returns an empty env map",
   :var-type "function",
   :line 79,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form env]),
   :name "macroexpand-1",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L133",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/macroexpand-1",
   :doc
   "If form represents a macro form or an inlineable function,\nreturns its expansion, else returns form.",
   :var-type "function",
   :line 133,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L59",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/parse",
   :namespace "clojure.tools.analyzer.jvm",
   :line 59,
   :var-type "multimethod",
   :doc "Extension to tools.analyzer/-parse for JVM special forms",
   :name "parse"}
  {:arglists ([ast]),
   :name "run-passes",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L372",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/run-passes",
   :doc
   "Applies the following passes in the correct order to the AST:\n* uniquify\n* add-binding-atom\n* cleanup\n* source-info\n* elide-meta\n* warn-earmuff\n* collect\n* jvm.box\n* jvm.constant-lifter\n* jvm.annotate-branch\n* jvm.annotate-loops\n* jvm.annotate-class-id\n* jvm.annotate-internal-name\n* jvm.annotate-methods\n* jvm.fix-case-test\n* jvm.clear-locals\n* jvm.classify-invoke\n* jvm.validate\n* jvm.infer-tag\n* jvm.annotate-tag\n* jvm.validate-loop-locals\n* jvm.analyze-host-expr",
   :var-type "function",
   :line 372,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/3db3fc86bf5e25d2735dca393c182aa1e93bfe06/src/main/clojure/clojure/tools/analyzer/jvm.clj#L54",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/specials",
   :namespace "clojure.tools.analyzer.jvm",
   :line 54,
   :var-type "var",
   :doc "Set of the special forms for clojure in the JVM",
   :name "specials"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L115",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/box",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 115,
   :var-type "var",
   :doc
   "If the argument is a primitive Class, returns its boxed equivalent,\notherwise returns the argument",
   :name "box"}
  {:arglists ([c1 c2]),
   :name "convertible?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L157",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/convertible?",
   :doc "Returns true if it's possible to convert from c1 to c2",
   :var-type "function",
   :line 157,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L51",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/maybe-class",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 51,
   :var-type "var",
   :doc
   "Takes a Symbol, String or Class and tires to resolve to a matching Class",
   :name "maybe-class"}
  {:arglists ([c]),
   :name "numeric?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L141",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/numeric?",
   :doc "Returns true if the given class is numeric",
   :var-type "function",
   :line 141,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([tag]),
   :name "prim-or-obj",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L295",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/prim-or-obj",
   :doc
   "If the given Class is a primitive, returns that Class, otherwise returns Object",
   :var-type "function",
   :line 295,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L97",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/primitive?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 97,
   :var-type "var",
   :doc
   "Returns non-nil if the argument represents a primitive Class other than Void",
   :name "primitive?"}
  {:arglists ([c1 c2]),
   :name "subsumes?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L147",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/subsumes?",
   :doc "Returns true if c2 is subsumed by c1",
   :var-type "function",
   :line 147,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([tags methods]),
   :name "try-best-match",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L310",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/try-best-match",
   :doc
   "Given a vector of arg tags and a collection of methods, tries to return the\nsubset of methods that match best the given tags",
   :var-type "function",
   :line 310,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L128",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/unbox",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 128,
   :var-type "var",
   :doc
   "If the argument is a Class with a primitive equivalent, returns that,\notherwise returns the argument",
   :name "unbox"}
  {:arglists ([from to]),
   :name "wider-primitive",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L180",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-primitive",
   :doc "Given two numeric primitive Classes, returns the wider one",
   :var-type "function",
   :line 180,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([tags]),
   :name "wider-tag",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L209",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-tag",
   :doc "Given a collection of Classes returns the wider one",
   :var-type "function",
   :line 209,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([from to]),
   :name "wider-tag*",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L187",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-tag*",
   :doc "Given two Classes returns the wider one",
   :var-type "function",
   :line 187,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/ab6ee74a83d589e1052ec4061bf9ed5f33ee6931/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L170",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-than",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 170,
   :var-type "var",
   :doc
   "If the argument is a numeric primitive Class, returns a set of primitive Classes\nthat are narrower than the given one",
   :name "wider-than"})}
