Changelog
========================================
Since tools.analyzer.jvm version are usually cut simultaneously with a tools.analyzer version, check also the tools.analyzer [CHANGELOG](https://github.com/clojure/tools.analyzer/blob/master/CHANGELOG.md) for changes on the corresponding version, since changes in that library will reflect on this one.
- - -
* Release 0.1.0-beta13 on ???
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
  * First beta releas
