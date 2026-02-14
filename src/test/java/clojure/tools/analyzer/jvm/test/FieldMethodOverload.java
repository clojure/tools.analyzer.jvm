package clojure.tools.analyzer.jvm.test;

public class FieldMethodOverload {
    public static final String doppelganger = "static-field";

    public static String doppelganger() {
        return "";
    }

    public static String doppelganger(int a, int b) {
        return "int-int";
    }
}
