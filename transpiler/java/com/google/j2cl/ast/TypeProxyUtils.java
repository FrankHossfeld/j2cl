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
package com.google.j2cl.ast;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility functions to transpile JDT TypeBinding to J2cl TypeDescriptor.
 */
public class TypeProxyUtils {
  /**
   * Creates a TypeDescriptor from a JDT TypeBinding.
   */
  public static TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    if (typeBinding.isArray()) {
      TypeDescriptor leafTypeDescriptor = createTypeDescriptor(typeBinding.getElementType());
      return leafTypeDescriptor.getForArray(typeBinding.getDimensions());
    }

    return TypeDescriptor.create(typeBinding);
  }

  static List<String> getPackageComponents(ITypeBinding typeBinding) {
    List<String> packageComponents = new ArrayList<>();
    if (typeBinding.getPackage() != null) {
      packageComponents = Arrays.asList(typeBinding.getPackage().getNameComponents());
    }
    return packageComponents;
  }

  static List<String> getClassComponents(ITypeBinding typeBinding) {
    List<String> classComponents = new LinkedList<>();
    if (typeBinding.isWildcardType() || typeBinding.isCapture()) {
      return Arrays.asList("?");
    }
    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      String simpleName;
      if (currentType.isLocal()) {
        // JDT binary name for local class is like package.components.EnclosingClass$1SimpleName
        // Extract the name after the last '$' as the class component here.
        String binaryName = currentType.getErasure().getBinaryName();
        simpleName = binaryName.substring(binaryName.lastIndexOf('$') + 1);
      } else if (currentType.isTypeVariable()) {
        if (currentType.getDeclaringClass() != null) {
          // If it is a class-level type variable, use the simple name (with prefix "C_") as the
          // current name component.
          simpleName = AstUtils.TYPE_VARIABLE_IN_TYPE_PREFIX + currentType.getName();
        } else {
          // If it is a method-level type variable, use the simple name (with prefix "M_") as the
          // current name component, and add declaringClass_declaringMethod as the next name
          // component, and set currentType to declaringClass for the next iteration.
          classComponents.add(0, AstUtils.TYPE_VARIABLE_IN_METHOD_PREFIX + currentType.getName());
          simpleName =
              currentType.getDeclaringMethod().getDeclaringClass().getName()
                  + "_"
                  + currentType.getDeclaringMethod().getName();
          currentType = currentType.getDeclaringMethod().getDeclaringClass();
        }
      } else {
        simpleName = currentType.getErasure().getName();
      }
      classComponents.add(0, simpleName);
      currentType = currentType.getDeclaringClass();
    }
    return classComponents;
  }

  static List<TypeDescriptor> getTypeArgumentDescriptors(ITypeBinding typeBinding) {
    List<TypeDescriptor> typeArgumentDescriptors = new ArrayList<>();
    if (typeBinding.isParameterizedType()) {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeArguments()));
    } else {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeParameters()));
      boolean isInstanceNestedClass =
          typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers());
      if (isInstanceNestedClass) {
        if (typeBinding.getDeclaringMethod() != null) {
          typeArgumentDescriptors.addAll(
              createTypeDescriptors(typeBinding.getDeclaringMethod().getTypeParameters()));
        }
        typeArgumentDescriptors.addAll(
            createTypeDescriptor(typeBinding.getDeclaringClass()).getTypeArgumentDescriptors());
      }
    }
    return typeArgumentDescriptors;
  }

  public static Visibility getVisibility(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      return Visibility.PUBLIC;
    } else if (Modifier.isProtected(modifiers)) {
      return Visibility.PROTECTED;
    } else if (Modifier.isPrivate(modifiers)) {
      return Visibility.PRIVATE;
    } else {
      return Visibility.PACKAGE_PRIVATE;
    }
  }

  public static boolean isInstanceMemberClass(ITypeBinding typeBinding) {
    return typeBinding.isMember() && !Modifier.isStatic(typeBinding.getModifiers());
  }

  public static boolean isInstanceNestedClass(ITypeBinding typeBinding) {
    return typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers());
  }

  /**
   * Returns true if {@code typeBinding} is a class that implements a JsFunction interface.
   */
  public static boolean isJsFunctionImplementation(ITypeBinding typeBinding) {
    if (typeBinding == null || !typeBinding.isClass()) {
      return false;
    }
    for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
      if (JsInteropUtils.isJsFunction(superInterface)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isEnumOrSubclass(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return false;
    }
    return typeBinding.isEnum() || isEnumOrSubclass(typeBinding.getSuperclass());
  }

  /**
   * Returns true if the given type has a JsConstructor.
   */
  public static boolean isJsConstructorClass(ITypeBinding typeBinding) {
    if (typeBinding == null || !typeBinding.isClass()) {
      return false;
    }
    Collection<IMethodBinding> constructors =
        Collections2.filter(
            Arrays.asList(typeBinding.getDeclaredMethods()),
            new Predicate<IMethodBinding>() {
              @Override
              public boolean apply(IMethodBinding method) {
                return method.isConstructor();
              }
            });
    if (constructors.isEmpty()) {
      // A JsType with default constructor is a JsConstructor class.
      return JsInteropUtils.isJsType(typeBinding);
    }
    return !Collections2.filter(
            constructors,
            new Predicate<IMethodBinding>() {
              @Override
              public boolean apply(IMethodBinding constructor) {
                return JsInteropUtils.isJsConstructor(constructor);
              }
            })
        .isEmpty();
  }

  /**
   * Returns true if the given type has a JsConstructor, or it is a successor of a class that has a
   * JsConstructor.
   */
  public static boolean subclassesJsConstructorClass(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return false;
    }
    return isJsConstructorClass(typeBinding)
        || subclassesJsConstructorClass(typeBinding.getSuperclass());
  }

  /**
   * Returns the MethodDescriptor for the SAM method of {@code typeBinding}.
   */
  public static MethodDescriptor getJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    if (!isJsFunctionImplementation(typeBinding)) {
      return null;
    }
    Preconditions.checkArgument(
        typeBinding.getInterfaces().length == 1,
        "Type %s should implement one and only one super interface.",
        typeBinding.getName());
    ITypeBinding jsFunctionInterface = typeBinding.getInterfaces()[0];
    Preconditions.checkArgument(
        jsFunctionInterface.getDeclaredMethods().length == 1,
        "Type %s should have one and only one method.",
        jsFunctionInterface.getName());
    IMethodBinding interfaceSAM = jsFunctionInterface.getDeclaredMethods()[0];
    // If it is a parametric method, returns the original generic method, which has the same
    // MethodDescriptor as the bridge method, and the JsFunction instance should return the bridge
    // method, which does a casting on arguments.
    return JdtMethodUtils.createMethodDescriptor(interfaceSAM.getMethodDeclaration());
  }

  private static List<TypeDescriptor> createTypeDescriptors(ITypeBinding[] typeBindings) {
    return Lists.transform(
        Arrays.asList(typeBindings),
        new Function<ITypeBinding, TypeDescriptor>() {
          @Override
          public TypeDescriptor apply(ITypeBinding typeBinding) {
            return createTypeDescriptor(typeBinding);
          }
        });
  }
}
