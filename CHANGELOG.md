Changelog
========================================
Since tools.analyzer.jvm version are usually cut simultaneously with a tools.analyzer version, check also the tools.analyzer [CHANGELOG](https://github.com/clojure/tools.analyzer/blob/master/CHANGELOG.md) for changes on the corresponding version, since changes in that library will reflect on this one.
- - -

* Release 0.7.0 on 14 Feb 2017
* Release 0.7.0-alpha1 on 26 Jan 2017
  * Added handle-evaluation-exception opts to `analyze+eval`
  * Changed `analyze+eval`'s default behaviour on eval exception
  * Stop caching maybe-class-from-string as it implicitely relies on dynamic state
  * Make analyze work from nested invocations -- remove state mutation
* Release 0.6.10 on 17 Jun 2016
  * Fix ns munging
* Release 0.6.9 on 10 Nov 2015
  * Correctly check for recur on case :then branches
* Release 0.6.8 on 3 Nov 2015
  * Fixed eof check in tools.reader usage
  * Avoid reflection on catch local
  * Fix context in analyze+eval statement
* Release 0.6.7 on 23 Apr 2015
  * Add support for reader conditionals
  * Ensure *file* is correctly bound in analyze-ns
  * Fixed emit-form for :host-interop
* Release 0.6.6 on 23 Feb 2015
  * Small performance enhancements
  * Added validate-recur pass
  * Renamed annotate-methods to annotate-host-info
  * Fixed class resolution
  * Added macroexpand-all
  * Fixed ::resolved-op handling in analyze+eval
* Release 0.6.5 on 20 Nov 2014
  * Ensure *ns* is correctly bound during analysis
  * Removed analyze' and analyze+eval'
  * Improvements in class resolution
* Release 0.6.4 on 03 Nov 2014
  * Disallow def of a symbol that maps to a Class
  * Made the target of a host interop expression privilege classnames over the lexical scope, as in clojure
  * Preserve correct meta on emit-form
  * Validate the target of a new expression
  * Fixed bug that caused the symbols used as primitive type hints to be interpreted as classes in host interop expressions
  * Made update-ns-map! an optional global-env field
  * Enhanced source-info support on analyze+eval
* Release 0.6.3 on 27 Oct 2014
  * Better interop method matcher
  * Fixed a bug when using analyze+eval and lein uberjar caused by Compiler/LOADER being unbound during macroexpansion
  * Faster maybe-class impl
* Release 0.6.1 on 13 Oct 2014
  * Significant performance enhancements
  * Made Class literals shadow Vars
  * Fixed a bug in :arglists automatic tag qualification
  * :env :locals are no longer uniquified by default, can be changed via pass-opts
  * Fixed tag validation
  * Removed annotate-class-id, annotate-internal-name, ensure-tag, collect, collect-closed-overs and clear-locals, moved to tools.emiter.jvm
  * Fixed a bug in the method resolution code, caused some unnecessary reflection
  * Added opts and env args to analyze-ns, consistent with the other analyze functions
  * Made emit-form with :qualified-symbols qualify def symbol
* Release 0.6.0 on 18 Sep 2014
  * Started using clojure.tools.analyzer.passes/schedule to schedule the default passes and configured all the passes
  * Reduced the set of default passes, removed: annotate-class-id, annotate-internal-name, ensure-tag
  * Changed the interface of the collect pass
  * Added default-passes and default-passes-opts to the clojure.tools.analyzer.jvm namespace
* Release 0.5.6 on 02 Sep 2014
  * Fixed a bug in classify-invoke that caused default-exprs in keyword invoke expressions to be lost
* Release 0.5.5 on 31 Aug 2014
  * Fixed analyze-ns analysis caching
  * Qualify :arglists class names
* Release 0.5.4 on 21 Aug 2014
  * Added optional unresolved symbol handler, configurable via :passes-opts
* Release 0.5.3 on 14 Aug 2014
  * Compare contexts with isa? rather than =
  * Fixed a reflection warning
  * Fixed a bug in the :protocol-invoke nodes that caused ast/children to crash
* Release 0.5.2 on 09 Aug 2014
  * Fixed emit-form
  * Imported collect pass from tools.analyzer
  * Fixed infer-tag for :def
* Release 0.5.1 on 09 Aug 2014
  * Allow ^:const values to be unboxed
  * Made :keyword a children in :keyword-invoke
  * Added optional Var tag inference, configurable via :passes-opts
  * Added optional wrong tag handler, configurable via :passes-opts
  * Added optional mismatched arity handler, configurable via :passes-opts
* Release 0.5.0 on 29 Jul 2014
  * BREAKING CHANGE: changed :protocol-invoke and :keyword-invoke fields
  * Made :host-interop :assignable?
* Release 0.4.0 on 26 Jul 2014
  * BREAKING CHANGE: update to new :class field for :new and :catch nodes
  * Elide source info metadata on :reify, :fn
  * Fixed performance regression
  * Added :qualified-symbols option to emit-form, deprecate :qualified-vars
  * Don't promote :invoke to :keyword-invoke when the keyword is namespaced
  * Added analyze-ns
  * Fixed some wrong contexts
  * Fixed and enhanced :tag/:arglists inference for :try nodes
  * Fixed handling of void bodies in loops
  * Collect closed-overs on :try
* Release 0.3.0 on 21 Jun 2014
  * BREAKING API CHANGE: Updated to new :context
  * Fixed 1-arity macroexpand-1
  * validate throws on Var not found
* Release 0.2.2 on 13 Jun 2014
  * Added 1-arity version of macroexpand-1
  * Made analyze+eval handle exceptions via ExceptionThrown
  * Fixed a bug in the validate pass that caused some instance-methods to stay unresolved
  * Keep :raw-forms on analyze+eval
  * Update \*ns\* in each call to analyze+eval
* Release 0.2.1 on 08 Jun 2014
  * Made run-passes dynamic
  * Made analyze-host-expr and classify-invoke preserve the original AST fields
* Release 0.2.0 on 05 Jun 2014
  * BREAKING API CHANGE: Updated to new global env interface
  * Made analyze+eval attach the result of evaluating the form to the AST
* Release 0.1.0-beta13 on 11 Mar 2014
  * Don't run cleanup on analyze, added analyze' and analyze+eval' that run it
  * Added :top-level true to constructed :do nodes
  * Added 3-arity to analyze taking an optional map of options
  * Fixes regarding :fn-method :o-tag/:tag handling
* Release 0.1.0-beta12 on 25 Apr 2014
  * Default to (empty-env) if env not provided
  * Fix a bug in check-recur with case
* Release 0.1.0-beta11 on 18 Apr 2014
  * Performance enhancements on reflection utils
  * Workaround for a weird behaviour of clojure.reflect on interfaces
  * Fix for annotate-tag and validate-loop-locals interaction
  * Improve logic of try-best-match
  * Improve handling of Void tag
  * Fix handling of tag on constructor that defer to runtime reflection
  * Fix validate-loop-locals when the return type of the loop changed after the invalidation
  * Added :qualified-vars option to emit-form
* Release 0.1.0-beta10 on 1 Apr 2014
  * Fix validate-loop-locals handling of tag
  * merge &form meta into mfrom meta to preserve source info during macroexpansion
* Release 0.1.0-beta9 on 29 Mar 2014
  * Macroexpand evaluates :inline/:inline-arities to allow using the inlined version
    in the fn body
  * Fix fn name munging
  * Fix annotate-loops handling of statements
  * Update the ns map in the env after macroexpansion as some interning might
    happen at macroexpansion time
  * Added analyze+eval
  * Pass (:locals env) as &env instead of env, macros that use (keys &env) now work
  * Fix binding init tag
  * Fix create-var handling of meta
* Release 0.1.0-beta8 on 11 Mar 2014
  * Removed :name in env for the :fn name, moved in a tools.analyzer.jvm pass
  * Added docstrings
  * Add annotate-internal-name pass
  * Add warn-on-reflection pass
  * clear-locals is *compiler-options* aware
* Release 0.1.0-beta7 on 28 Feb 2014
  * Moved :should-not-clear annotation from annotate-branch to clear-locals
* Release 0.1.0-beta6 on 27 Feb 2014
  * Bind Compiler/LOADER to a new DynamicClassLoader on every analyze call to avoid
    problems regarding deftype redefinitions
  * Fix handling of meta by create-var
* Release 0.1.0-beta5 on 26 Feb 2014
  * Clear :catch locals
  * Added "this" clearing where possible (CLJ-1250)
  * Clear unused bindings
  * Attach the correct :tag on instance call/field instances
  * Fixes to clear-locals pass regarding nested loops
* Release 0.1.0-beta4 on 17 Feb 2014
  * Fix validate-loop-locals to short-circuit on nested loops
  * Added docstrings
  * Correctly clear closed-overs on :once fns
  * Correctly clear closed-overs used in closure creation
* Release 0.1.0-beta3 on 15 Feb 2014
  * Added annotate-class-id
  * clear-locals clears loop locals when possible
* Release 0.1.0-beta2 on 14 Feb 2014
  * Memoize only maybe-class and member*, a new deftype invalidates the cache
* Release 0.1.0-beta1 on 11 Feb 2014
  * First beta release
