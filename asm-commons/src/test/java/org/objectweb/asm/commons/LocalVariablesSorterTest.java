// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.objectweb.asm.test.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.test.AsmTest;
import org.objectweb.asm.tree.MethodNode;

/**
 * LocalVariablesSorter tests.
 *
 * @author Eric Bruneton
 */
public class LocalVariablesSorterTest extends AsmTest {

  @Test
  public void testConstructor() {
    new LocalVariablesSorter(Opcodes.ACC_PUBLIC, "()V", new MethodNode());
    assertThrows(
        IllegalStateException.class,
        () -> new LocalVariablesSorter(Opcodes.ACC_PUBLIC, "()V", new MethodNode()) {});
  }

  @Test
  public void testVisitFrame() {
    LocalVariablesSorter localVariablesSorter =
        new LocalVariablesSorter(Opcodes.ACC_PUBLIC, "()V", new MethodNode());
    localVariablesSorter.visitFrame(Opcodes.F_NEW, 0, null, 0, null);
    assertThrows(
        IllegalArgumentException.class,
        () -> localVariablesSorter.visitFrame(Opcodes.F_FULL, 0, null, 0, null));
  }

  @Test
  public void testNewLocal() {
    LocalVariablesSorter localVariablesSorter =
        new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.BOOLEAN_TYPE);
    assertEquals(1, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.BYTE_TYPE);
    assertEquals(2, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.CHAR_TYPE);
    assertEquals(3, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.SHORT_TYPE);
    assertEquals(4, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.INT_TYPE);
    assertEquals(5, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.FLOAT_TYPE);
    assertEquals(6, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.LONG_TYPE);
    assertEquals(8, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.DOUBLE_TYPE);
    assertEquals(10, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.getObjectType("pkg/Class"));
    assertEquals(11, localVariablesSorter.nextLocal);
    localVariablesSorter.newLocal(Type.getType("[I"));
    assertEquals(12, localVariablesSorter.nextLocal);
  }

  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testSortLocalVariablesAndInstantiate(
      final PrecompiledClass classParameter, final Api apiParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor classVisitor =
        new ClassVisitor(apiParameter.value(), classWriter) {
          @Override
          public MethodVisitor visitMethod(
              final int access,
              final String name,
              final String descriptor,
              final String signature,
              final String[] exceptions) {
            return new LocalVariablesSorter(
                api,
                access,
                descriptor,
                super.visitMethod(access, name, descriptor, signature, exceptions));
          }
        };

    if (classParameter.isMoreRecentThan(apiParameter)) {
      assertThrows(
          RuntimeException.class,
          () -> classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES));
      return;
    }

    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
    assertThat(() -> loadAndInstantiate(classParameter.getName(), classWriter.toByteArray()))
        .succeedsOrThrows(UnsupportedClassVersionError.class)
        .when(classParameter.isMoreRecentThanCurrentJdk());
  }

  @Test
  public void testSortLocalVariablesAndInstantiate() throws FileNotFoundException, IOException {
    ClassReader classReader =
        new ClassReader(Files.newInputStream(Paths.get("src/test/resources/Issue317586.class")));
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor classVisitor =
        new ClassVisitor(Opcodes.ASM7, classWriter) {
          @Override
          public MethodVisitor visitMethod(
              final int access,
              final String name,
              final String descriptor,
              final String signature,
              final String[] exceptions) {
            return new LocalVariablesSorter(
                api,
                access,
                descriptor,
                super.visitMethod(access, name, descriptor, signature, exceptions));
          }
        };
    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
    loadAndInstantiate("app1.Main$BadLocal", classWriter.toByteArray());
  }
}
