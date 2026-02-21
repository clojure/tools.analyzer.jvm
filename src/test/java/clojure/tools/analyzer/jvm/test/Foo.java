package clojure.tools.analyzer.jvm.test;

import clojure.lang.AFn;
import clojure.lang.IFn;

public class Foo {

  public static final IFn bar = new AFn() {
      public Object invoke() {
        return "bar";
      }
      public Object invoke(Object x) {
        return "bar" + x.toString();
      }
    };

  public static final IFn baz = new AFn() {
      public Object invoke() {
        return "baz";
      }
      public Object invoke(Object x) {
        return "baz" + x.toString();
      }
    };

  public static String bar() {
    return "bar()";
  }
  public static String bar(Object x) {
    return "bar()" + x.toString();
  }


  public static String qux() {
    return "qux()";
  }
  public static String qux(Object x) {
    return "qux()" + x.toString();
  }

}
