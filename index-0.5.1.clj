{:namespaces
 ({:source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm/clojure.tools.analyzer.jvm-api.html",
   :name "clojure.tools.analyzer.jvm",
   :doc
   "Analyzer for clojure code, extends tools.analyzer with JVM specific passes/forms"}
  {:source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm/clojure.tools.analyzer.jvm.utils-api.html",
   :name "clojure.tools.analyzer.jvm.utils",
   :doc nil}),
 :vars
 ({:arglists ([e]),
   :name "->ExceptionThrown",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L490",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/->ExceptionThrown",
   :doc
   "Positional factory function for class clojure.tools.analyzer.jvm.ExceptionThrown.",
   :var-type "function",
   :line 490,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form] [form env] [form env opts]),
   :name "analyze",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L459",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze",
   :doc
   "Returns an AST for the form that's compatible with what tools.emitter.jvm requires.\n\nBinds tools.analyzer/{macroexpand-1,create-var,parse} to\ntools.analyzer.jvm/{macroexpand-1,create-var,parse} and calls\ntools.analyzer/analyzer on form.\n\nIf provided, opts should be a map of options to analyze, currently the only valid option\nis :bindings.\nIf provided, :bindings should be a map of Var->value pairs that will be merged into the\ndefault bindings for tools.analyzer, useful to provide custom extension points.\n\nE.g.\n(analyze form env {:bindings  {#'ana/macroexpand-1 my-mexpand-1}})\n\nCalls `run-passes` on the AST.",
   :var-type "function",
   :line 459,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form] [form env] [form env opts]),
   :name "analyze'",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L550",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze'",
   :doc "Like `analyze` but runs cleanup on the AST",
   :var-type "function",
   :line 550,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form] [form env] [form env opts]),
   :name "analyze+eval",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L503",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze+eval",
   :doc
   "Like analyze but evals the form after the analysis and attaches the\nreturned value in the :result field of the AST node.\nIf evaluating the form will cause an exception to be thrown, the exception\nwill be caught and the :result field will hold an ExceptionThrown instance\nwith the exception in the \"e\" field.\n\nUseful when analyzing whole files/namespaces.",
   :var-type "function",
   :line 503,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form] [form env] [form env opts]),
   :name "analyze+eval'",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L557",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze+eval'",
   :doc "Like `analyze+eval` but runs cleanup on the AST",
   :var-type "function",
   :line 557,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([ns]),
   :name "analyze-ns",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L564",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/analyze-ns",
   :doc
   "Analyzes a whole namespace, returns a vector of the ASTs for all the\ntop-level ASTs of that namespace.\nEvaluates all the forms.",
   :var-type "function",
   :line 564,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([s]),
   :name "butlast+last",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L492",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/butlast+last",
   :doc
   "Returns same value as (juxt butlast last), but slightly more\nefficient since it only traverses the input sequence s once, not\ntwice.",
   :var-type "function",
   :line 492,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([sym {:keys [ns]}]),
   :name "create-var",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L181",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/create-var",
   :doc
   "Creates a Var for sym and returns it.\nThe Var gets interned in the env namespace.",
   :var-type "function",
   :line 181,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([]),
   :name "empty-env",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L86",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/empty-env",
   :doc "Returns an empty env map",
   :var-type "function",
   :line 86,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:arglists ([form] [form env]),
   :name "macroexpand-1",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L139",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/macroexpand-1",
   :doc
   "If form represents a macro form or an inlineable function,\nreturns its expansion, else returns form.",
   :var-type "function",
   :line 139,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L64",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/parse",
   :namespace "clojure.tools.analyzer.jvm",
   :line 64,
   :var-type "multimethod",
   :doc "Extension to tools.analyzer/-parse for JVM special forms",
   :name "parse"}
  {:arglists ([ast]),
   :name "run-passes",
   :namespace "clojure.tools.analyzer.jvm",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L385",
   :dynamic true,
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/run-passes",
   :doc
   "Applies the following passes in the correct order to the AST:\n* uniquify\n* add-binding-atom\n* cleanup\n* source-info\n* elide-meta\n* warn-earmuff\n* collect\n* jvm.box\n* jvm.constant-lifter\n* jvm.annotate-branch\n* jvm.annotate-loops\n* jvm.annotate-class-id\n* jvm.annotate-internal-name\n* jvm.annotate-methods\n* jvm.fix-case-test\n* jvm.clear-locals\n* jvm.classify-invoke\n* jvm.validate\n* jvm.infer-tag\n* jvm.annotate-tag\n* jvm.validate-loop-locals\n* jvm.analyze-host-expr",
   :var-type "function",
   :line 385,
   :file "src/main/clojure/clojure/tools/analyzer/jvm.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/28f52ebe19705740531bbdf0eda9cdad408aa057/src/main/clojure/clojure/tools/analyzer/jvm.clj#L59",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/specials",
   :namespace "clojure.tools.analyzer.jvm",
   :line 59,
   :var-type "var",
   :doc "Set of the special forms for clojure in the JVM",
   :name "specials"}
  {:file nil,
   :raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm/ExceptionThrown",
   :namespace "clojure.tools.analyzer.jvm",
   :var-type "type",
   :name "ExceptionThrown"}
  {:arglists ([c]),
   :name "box",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L118",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/box",
   :doc
   "If the argument is a primitive Class, returns its boxed equivalent,\notherwise returns the argument",
   :var-type "function",
   :line 118,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([c1 c2]),
   :name "convertible?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L164",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/convertible?",
   :doc "Returns true if it's possible to convert from c1 to c2",
   :var-type "function",
   :line 164,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L54",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/maybe-class",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 54,
   :var-type "var",
   :doc
   "Takes a Symbol, String or Class and tires to resolve to a matching Class",
   :name "maybe-class"}
  {:arglists ([c]),
   :name "numeric?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L148",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/numeric?",
   :doc "Returns true if the given class is numeric",
   :var-type "function",
   :line 148,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([tag]),
   :name "prim-or-obj",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L302",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/prim-or-obj",
   :doc
   "If the given Class is a primitive, returns that Class, otherwise returns Object",
   :var-type "function",
   :line 302,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L100",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/primitive?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 100,
   :var-type "var",
   :doc
   "Returns non-nil if the argument represents a primitive Class other than Void",
   :name "primitive?"}
  {:arglists ([c1 c2]),
   :name "subsumes?",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L154",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/subsumes?",
   :doc "Returns true if c2 is subsumed by c1",
   :var-type "function",
   :line 154,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([tags methods]),
   :name "try-best-match",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L317",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/try-best-match",
   :doc
   "Given a vector of arg tags and a collection of methods, tries to return the\nsubset of methods that match best the given tags",
   :var-type "function",
   :line 317,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([c]),
   :name "unbox",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L133",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/unbox",
   :doc
   "If the argument is a Class with a primitive equivalent, returns that,\notherwise returns the argument",
   :var-type "function",
   :line 133,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([from to]),
   :name "wider-primitive",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L187",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-primitive",
   :doc "Given two numeric primitive Classes, returns the wider one",
   :var-type "function",
   :line 187,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([tags]),
   :name "wider-tag",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L216",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-tag",
   :doc "Given a collection of Classes returns the wider one",
   :var-type "function",
   :line 216,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:arglists ([from to]),
   :name "wider-tag*",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L194",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-tag*",
   :doc "Given two Classes returns the wider one",
   :var-type "function",
   :line 194,
   :file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj"}
  {:file "src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :raw-source-url
   "https://github.com/clojure/tools.analyzer.jvm/raw/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj",
   :source-url
   "https://github.com/clojure/tools.analyzer.jvm/blob/341f9b4a0bc5ac055283551039c32a042ade04e2/src/main/clojure/clojure/tools/analyzer/jvm/utils.clj#L177",
   :wiki-url
   "http://clojure.github.com/tools.analyzer.jvm//clojure.tools.analyzer.jvm-api.html#clojure.tools.analyzer.jvm.utils/wider-than",
   :namespace "clojure.tools.analyzer.jvm.utils",
   :line 177,
   :var-type "var",
   :doc
   "If the argument is a numeric primitive Class, returns a set of primitive Classes\nthat are narrower than the given one",
   :name "wider-than"})}
