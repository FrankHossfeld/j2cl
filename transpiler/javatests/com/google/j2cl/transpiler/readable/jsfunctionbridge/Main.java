package com.google.j2cl.transpiler.readable.jsfunctionbridge;

import jsinterop.annotations.JsFunction;

public class Main {
  public static void test() {
    ApplyFunction<String> foo =
        new ApplyFunction<String>() {
          public String field = "abc";

          @Override
          public String apply(String s) {
            return s + field;
          }

          public String fun() {
            return field + apply("a");
          }
        };
    callGeneric(foo, "a");
    callParametric(foo, "a");
    foo.apply("a");
  }

  public static void callGeneric(ApplyFunction af, Object o) {
    af.apply(o);
  }

  public static void callParametric(ApplyFunction<String> af, String s) {
    af.apply(s);
  }

  @JsFunction
  interface ApplyFunction<T> {
    T apply(T element);
  }
}
