

bazel build \\transpiler\java\com\google\j2cl\transpiler:*

bazel build \\tools\java\com\google\j2cl\tools\gwtincompatible:*
bazel build \\tools\java\com\google\j2cl\tools\minifier:*

bazel build \\jre\java\javaemul\internal\vmbootstrap\primitives:primitives

bazel build \\jre\java:*

bazel build \\junit\emul\java:*
bazel build \\jre\javatests\com\google\gwt\junit:*
bazel build \\junit\generator\java\com\google\j2cl\junit\apt:*

bazel build @org_gwtproject_gwt\\user:libgwt-javaemul-internal-annotations.jar
bazel build @org_gwtproject_gwt\\user:libgwt-javaemul-internal-annotations-src.jar

## This must be the last line, or else some other operation will apparently remove these
## soft links that we rely on in the maven build
bazel build \\third_party:jdt-core 