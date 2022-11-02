package com.github.forax.recordmap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM9;

public class Rewriter {
  private static String resource(Class<?> type) {
    return "/" + type.getName().replace('.', '/') + ".class";
  }

  private static Map<String, String> gatherSignatureMap(Class<?> type) throws IOException {
    var resource = resource(type);
    byte[] bytecode;
    try(var inputStream = type.getResourceAsStream(resource)) {
      if (inputStream == null) {
        throw new IOException("class " + type.getName() + " bytecode not found");
      }
      bytecode = inputStream.readAllBytes();
    }

    var map = new HashMap<String, String>();
    var reader = new ClassReader(bytecode);
    reader.accept(
        new ClassVisitor(ASM9) {
          @Override
          public MethodVisitor visitMethod(int access, String name, String descriptor,
                                           String signature, String[] exceptions) {
            if (signature != null) {
              map.put(name + descriptor, signature);
            }
            return null;
          }
        },
        0);
    return map;
  }

  private static byte[] patch(Path path, Map<String, String> signatureMap) throws IOException {
    byte[] bytecode;
    try (var inputStream = Files.newInputStream(path)) {
      bytecode = inputStream.readAllBytes();
    }

    var reader = new ClassReader(bytecode);
    var writer = new ClassWriter(reader, 0);
    reader.accept(
        new ClassVisitor(ASM9, writer) {
          @Override
          public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            var patchedSignature = "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/lang/Iterable<Lcom/github/forax/recordmap/RecordMap$RecordEntry<TK;TV;>;>;";
            System.out.println("rewrite class signature from " + signature + " to " + patchedSignature);
            super.visit(version, access, name, patchedSignature, superName, interfaces);
          }

          @Override
          public MethodVisitor visitMethod(
              int access, String name, String descriptor, String signature, String[] exceptions) {
            var patchedSignature = signatureMap.get(name + descriptor);
            if (patchedSignature != null) {
              if (!patchedSignature.equals(signature)) {
                System.out.println("rewrite " + name + descriptor + " from " + signature + " to " + patchedSignature);
              }
              signature = patchedSignature;
            }
            return writer.visitMethod(access, name, descriptor, signature, exceptions);
          }
        },
        0);
    return writer.toByteArray();
  }

  public static void main(String[] args) throws IOException {
    var recordMapPath = Path.of("target/classes" + resource(RecordMap.class));

    var map = gatherSignatureMap(Map.class);

    // do not rewrite entrySet()
    map.remove("entrySet()Ljava/util/Set;");

    var bytecode = patch(recordMapPath, map);
    Files.write(recordMapPath, bytecode);
  }
}
