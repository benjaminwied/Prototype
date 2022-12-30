/**
 * MIT License
 *
 * Copyright (c) 2022 Benjamin Wied
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.cleverelephant.prototype.materialization;

import com.github.cleverelephant.prototype.PrototypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.*;

/**
 * A class visitor that generates a concrete Prototype implementation while visiting the Prototype interface. Classes
 * are generated in package {@code com.github.cleverelephant.prototyp.materialization.generated} with the same name as
 * the implemented Prototype.
 *
 * @author Benjamin Wied
 */
public class MaterializingClassVisitor extends ClassVisitor
{
    private static final String CLASS_CONSTRUCTOR_NAME = "<clinit>";
    private static final String CONSTRUCTOR_NAME = "<init>";
    private static final String INTERNAL_PROTOTYPE_EXCEPTION_NAME = PrototypeException.class.getName()
            .replace('.', '/');

    private final ClassWriter classWriter;
    private final Consumer<Class<?>> classMaterializer;
    private final Map<String, GetterData> getters;
    private String className;

    /**
     * Constructs a new MaterializingClassVisitor.
     *
     * @param  classMaterializer
     *                              to materialize super-prototypes (if any)
     *
     * @throws NullPointerException
     *                              if classMaterializer is null
     */
    public MaterializingClassVisitor(Consumer<Class<?>> classMaterializer)
    {
        super(ASM9);
        this.classMaterializer = Objects.requireNonNull(classMaterializer, "classMaterializer must not be null");
        getters = new HashMap<>();
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
    {
        className = "com/github/cleverelephant/prototype/materialization/generated/" + name;

        String superClassName;
        if ("com/github/cleverelephant/prototype/Prototype".equals(name))
            superClassName = "java/lang/Object";
        else {
            String superInterface = interfaces[0];
            superClassName = "com/github/cleverelephant/prototype/materialization/generated/" + superInterface;

            /* class superClassName may not be materialized yet, so materialize it first */
            materializedSuperInterface(superInterface);
        }

        classWriter.visit(version, ACC_PUBLIC, className, null, superClassName, new String[] { name });

        /* Add constructor */
        MethodVisitor constructor = classWriter.visitMethod(ACC_PUBLIC, CONSTRUCTOR_NAME, "()V", null, null);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, superClassName, CONSTRUCTOR_NAME, "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
    }

    private void materializedSuperInterface(String superInterface)
    {
        try {
            Class<?> superInterfaceClass = Class.forName(superInterface.replace('/', '.'));
            classMaterializer.accept(superInterfaceClass);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("failed to materialized super interface", e);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
    {
        if (CLASS_CONSTRUCTOR_NAME.equals(name) || CONSTRUCTOR_NAME.equals(name))
            return null;
        if (name.startsWith("$")) {
            String defName = name.substring(1);
            getters.compute(defName, (__, data) -> {
                if (data == null)
                    data = new GetterData();
                data.hasDefaultValue = true;
                return data;
            });

            return null;
        }

        if (signature != null && !signature.startsWith("()") || !descriptor.startsWith("()"))
            throw new IllegalArgumentException();

        String fieldDescriptor = descriptor.substring(2);
        String fieldSignature = signature != null ? signature.substring(2) : null;

        return generate(name, fieldDescriptor, fieldSignature);
    }

    private MethodVisitor generate(String name, String fieldDescriptor, String fieldSignature)
    {
        classWriter.visitField(ACC_PRIVATE, name, fieldDescriptor, fieldSignature, null);
        classWriter.visitField(ACC_PRIVATE, "$" + name, "B", null, null);

        getters.compute(name, (__, data) -> {
            if (data == null)
                data = new GetterData();
            data.name = name;
            data.fieldDescriptor = fieldDescriptor;
            data.fieldSignature = fieldSignature;
            return data;
        });
        return generateSetter(name, fieldDescriptor, fieldSignature);
    }

    private MethodVisitor generateSetter(String name, String fieldDescriptor, String fieldSignature)
    {
        String methodSignature = fieldSignature != null ? "(" + fieldSignature + ")V" : null;
        MethodVisitor setter = classWriter
                .visitMethod(ACC_PUBLIC, name, "(" + fieldDescriptor + ")V", methodSignature, null);

        return new MethodVisitor(api) {
            private boolean hasJsonPropertyAnnotation;

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
            {
                if (JsonProperty.class.descriptorString().equals(descriptor)) {
                    hasJsonPropertyAnnotation = true;

                    /* Do not add JsonPropertyAnnotation for name property */
                    if ("name".equals(name))
                        return null;
                }

                return setter.visitAnnotation(descriptor, visible);
            }

            @Override
            public void visitEnd()
            {
                if (!hasJsonPropertyAnnotation && !"name".equals(name)) {
                    AnnotationVisitor visitor = setter.visitAnnotation(JsonProperty.class.descriptorString(), true);
                    visitor.visit("value", name);
                    visitor.visitEnd();
                }

                /*
                 * this.name = name;
                 */
                setter.visitVarInsn(ALOAD, 0);
                setter.visitVarInsn(Util.loadOpcode(fieldDescriptor), 1);
                setter.visitFieldInsn(PUTFIELD, className, name, fieldDescriptor);

                /*
                 * $name = true;
                 */
                setter.visitVarInsn(ALOAD, 0);
                setter.visitLdcInsn(true);
                setter.visitFieldInsn(PUTFIELD, className, "$" + name, "B");

                setter.visitInsn(RETURN);
                setter.visitMaxs(0, 0);
                setter.visitEnd();
            }
        };
    }

    private static class GetterData
    {
        private String name;
        private String fieldDescriptor;
        private String fieldSignature;

        private boolean hasDefaultValue;
    }

    private void generateGetter(GetterData data)
    {
        if (data.name == null)
            return;

        String methodSignature = data.fieldSignature != null ? "()" + data.fieldSignature : null;
        MethodVisitor getter = classWriter
                .visitMethod(ACC_PUBLIC, data.name, "()" + data.fieldDescriptor, methodSignature, null);

        /*
         * if($name)
         */
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className, "$" + data.name, "B");
        Label label = new Label();
        getter.visitJumpInsn(Opcodes.IFEQ, label);

        /*
         * return name;
         */
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className, data.name, data.fieldDescriptor);
        getter.visitInsn(Util.returnOpcode(data.fieldDescriptor));

        getter.visitLabel(label);
        if (data.hasDefaultValue) {
            /*
             * else return $name();
             */
            getter.visitVarInsn(ALOAD, 0);
            getter.visitMethodInsn(INVOKEVIRTUAL, className, "$" + data.name, "()" + data.fieldDescriptor, false);
            getter.visitInsn(Util.returnOpcode(data.fieldDescriptor));
        } else {
            /*
             * throw new PrototypeException("no value present for property " + data.name);
             */
            getter.visitTypeInsn(NEW, INTERNAL_PROTOTYPE_EXCEPTION_NAME);
            getter.visitInsn(DUP);
            getter.visitLdcInsn("no value present for property " + data.name);
            getter.visitMethodInsn(
                    INVOKESPECIAL, INTERNAL_PROTOTYPE_EXCEPTION_NAME, "<init>", "(Ljava/lang/String;)V", false
            );
            getter.visitInsn(ATHROW);
        }

        getter.visitMaxs(0, 0);
        getter.visitEnd();
    }

    @Override
    public void visitEnd()
    {
        getters.forEach((__, data) -> generateGetter(data));
        classWriter.visitEnd();
    }

    /**
     * Converts the generated class to a byte array.
     *
     * @return the class data
     *
     * @see    ClassWriter#toByteArray()
     */
    public byte[] toByteArray()
    {
        return classWriter.toByteArray();
    }

}
