/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.integration;

import static com.google.j2cl.transpiler.integration.TranspilerTester.newTester;
import static com.google.j2cl.transpiler.integration.TranspilerTester.newTesterWithDefaults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import junit.framework.TestCase;

/** End to end test for command line invocations. */
public class CommandLineInvocationTest extends TestCase {
  public void testHelpFlag() {
    newTesterWithDefaults()
        .setArgs("-help")
        .assertTranspileSucceeds()
        // Just a smoke test to verify that we printing some flags with description
        .assertOutputStreamContainsSnippets("-classpath", "Specifies where to find");
  }

  public void testInvalidFlag() {
    newTesterWithDefaults()
        .setArgs("-unknown", "abc")
        .assertTranspileFails()
        .assertErrorsContainsSnippets(
            "\"-unknown\" is not a valid option\n"
                + "Usage: j2cl <options> <source files>\n"
                + "use -help for a list of possible options");
  }

  public void testFrontendFlag() throws IOException {
    newTesterWithDefaults()
        .addArgs("-frontend", "llama")
        .addCompilationUnit("Foo", "public class Foo {}")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("\"llama\" is not a valid value for \"-frontend");

    newTesterWithDefaults()
        .addArgs("-frontend", "jdt")
        .setOutputPath(Files.createTempFile("output", ".zip"))
        .addCompilationUnit("Foo", "public class Foo {}")
        .assertTranspileSucceeds();
  }

  public void testSyntaxError() {
    newTesterWithDefaults()
        .addCompilationUnit("SyntaxError", "public class SyntaxError {", "  =", "}")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("Syntax error on token \"=\"");
  }

  public void testInvalidOutputLocation() throws IOException {
    // Output to a location that is not a directory and is not a zip file.
    Path outputLocation = Files.createTempFile("foo", ".txt");
    newTesterWithDefaults()
        .addCompilationUnit("Foo", "public class Foo {}")
        .setOutputPath(outputLocation)
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Output location '" + outputLocation + "' must be a directory or .zip file.");
  }

  public void testMissingJreDependency() {
    // Create a clean tester so that the JRE dependency is not automatically added.
    newTester()
        .addCompilationUnit("EmptyClass", "public class EmptyClass {}")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("The type java.lang.Object cannot be resolved.");
  }

  public void testMissingSrcjar() {
    newTesterWithDefaults()
        .addArgs(File.separator + "foo" + File.separator + "bar" + File.separator + "Missing.srcjar")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("File '" + File.separator + "foo" + File.separator + "bar" + File.separator + "Missing.srcjar' not found.");
  }

  public void testCorruptSrcjar() {
    newTesterWithDefaults()
        .addFile("corrupt.srcjar", "This is not what an srcjar/zip file should contain.")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("Cannot extract zip")
        .assertErrorsContainsSnippets("/corrupt.srcjar");
  }

  public void testNoMatchingSource() {
    newTesterWithDefaults()
        .setJavaPackage("nativeclasstest")
        .addCompilationUnit(
            "NativeClass",
            "public class NativeClass {",
            "  public native void nativeInstanceMethod();",
            "}")
        .addNativeFile(
            "BadNameNativeClass", "NativeClass.prototype.m_nativeInstanceMethod = function () {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Cannot find matching native file 'nativeclasstest/NativeClass.native.js'.");
  }

  public void testNativeJsFileForJsEnum() {
    newTesterWithDefaults()
        .setJavaPackage("nativeclasstest")
        .addCompilationUnit(
            "ClosureEnum",
            "import jsinterop.annotations.*;",
            "@JsEnum",
            "public enum ClosureEnum{",
            "  OK,",
            "  CANCEL",
            "}")
        .addNativeFile("ClosureEnum", "const ClosureEnum ={ OK : 'OK', CANCEL : 'Cancel' }")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "JsEnum 'ClosureEnum' does not support having a '.native.js' file.");
  }

  public void testNativeJsFileForNativeJsEnum() {
    newTesterWithDefaults()
        .setJavaPackage("nativeclasstest")
        .addCompilationUnit(
            "ClosureEnum",
            "import jsinterop.annotations.*;",
            "@JsEnum(isNative=true)",
            "public enum ClosureEnum{",
            "  OK,",
            "  CANCEL",
            "}")
        .addNativeFile("ClosureEnum", "const ClosureEnum ={ OK : 'OK', CANCEL : 'Cancel' }")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "JsEnum 'ClosureEnum' does not support having a '.native.js' file.");
  }

  public void testNativeJsFileForNativeJsType() {
    newTesterWithDefaults()
        .setJavaPackage("nativeclasstest")
        .addCompilationUnit(
            "NativeClass",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true)",
            "public class NativeClass{",
            "}")
        .addNativeFile("NativeClass", "Class NativeClass{}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'NativeClass' does not support having a '.native.js' file.");
  }

  public void testUnusedSource() {
    newTesterWithDefaults()
        .setJavaPackage("nativeclasstest")
        .addCompilationUnit(
            "NativeClass",
            "public class NativeClass {",
            "  public native void nativeInstanceMethod();",
            "}")
        .addNativeFile(
            "NativeClass", "NativeClass.prototype.m_nativeInstanceMethod = function () {}")
        .addNativeFile("ExtraClass", "ExtraClass.prototype.m_nativeInstanceMethod = function () {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Unused native file 'nativeclasstest/ExtraClass.native.js'.");
  }

  public void testOutputsToDirectory() throws IOException {
    Path outputLocation = Files.createTempDirectory("outputdir");
    newTesterWithDefaults()
        .setOutputPath(outputLocation)
        .setJavaPackage("test")
        .addCompilationUnit("Foo", "public class Foo {", "  public class InnerFoo {}", "}")
        .addCompilationUnit("Bar", "public class Bar {", "  public class InnerBar {}", "}")
        .assertTranspileSucceeds()
        .assertOutputFilesExist(
            "test/Foo.java.js",
            "test/Foo.impl.java.js",
            "test/Bar.java.js",
            "test/Bar.impl.java.js")
        .assertOutputFilesDoNotExist("some/thing/Bogus.js");

    // Test transpilation of java file without java package
    newTesterWithDefaults()
        .setOutputPath(outputLocation)
        .setJavaPackage("")
        .addCompilationUnit("Foo", "public class Foo {}")
        .assertTranspileSucceeds()
        .assertOutputFilesExist("Foo.java.js", "Foo.impl.java.js");
  }

  public void testOutputsToZipFile() throws IOException {
    Path outputLocation = Files.createTempFile("output", ".zip");
    newTesterWithDefaults()
        .setOutputPath(outputLocation)
        .setJavaPackage("test")
        .addCompilationUnit("Foo", "public class Foo {", "  public class InnerFoo {}", "}")
        .addCompilationUnit("Bar", "public class Bar {", "  public class InnerBar {}", "}")
        .assertTranspileSucceeds();

    try (ZipFile zipFile = new ZipFile(outputLocation.toFile())) {
      assertNotNull(zipFile.getEntry("test/Foo.java.js"));
      assertNotNull(zipFile.getEntry("test/Foo.impl.java.js"));
      assertNotNull(zipFile.getEntry("test/Bar.java.js"));
      assertNotNull(zipFile.getEntry("test/Bar.impl.java.js"));
      assertNull(zipFile.getEntry("some/thing/Bogus.js"));
    }
  }
}
