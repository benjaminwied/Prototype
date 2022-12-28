package com.github.cleverelephant.prototype.materialization;

import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.*;

public class MaterializingClassVisitor extends ClassVisitor
{
    private static final String CLASS_CONSTRUCTOR_NAME = "<clinit>";
    private static final String CONSTRUCTOR_NAME = "<init>";

    private final ClassWriter classWriter;
    private final Consumer<Class<?>> classMaterializer;
    private String className;

    public MaterializingClassVisitor(Consumer<Class<?>> classMaterializer)
    {
        super(ASM9);
        this.classMaterializer = classMaterializer;
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
        if (name.startsWith("$") || CLASS_CONSTRUCTOR_NAME.equals(name) || CONSTRUCTOR_NAME.equals(name))
            return null;

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

        generateGetter(name, fieldDescriptor, fieldSignature);
        return generateSetter(name, fieldDescriptor, fieldSignature);
    }

    public MethodVisitor generateSetter(String name, String fieldDescriptor, String fieldSignature)
    {
        return new MethodVisitor(api) {
            private MethodVisitor setter;
            private boolean hasJsonPropertyAnnotation;

            {
                String methodSignature = fieldSignature != null ? "(" + fieldSignature + ")V" : null;
                setter = classWriter.visitMethod(ACC_PUBLIC, name, "(" + fieldDescriptor + ")V", methodSignature, null);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
            {
                if (JsonProperty.class.descriptorString().equals(descriptor))
                    hasJsonPropertyAnnotation = true;

                return setter.visitAnnotation(descriptor, visible);
            }

            @Override
            public void visitEnd()
            {
                if (!hasJsonPropertyAnnotation) {
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

    public void generateGetter(String name, String fieldDescriptor, String fieldSignature)
    {
        String methodSignature = fieldSignature != null ? "()" + fieldSignature : null;
        MethodVisitor getter = classWriter.visitMethod(ACC_PUBLIC, name, "()" + fieldDescriptor, methodSignature, null);

        /*
         * if($name)
         */
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className, "$" + name, "B");
        Label label = new Label();
        getter.visitJumpInsn(Opcodes.IFEQ, label);

        /*
         * return name;
         */
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className, name, fieldDescriptor);
        getter.visitInsn(Util.returnOpcode(fieldDescriptor));

        /*
         * else return $name();
         */
        getter.visitLabel(label);
        getter.visitVarInsn(ALOAD, 0);
        getter.visitMethodInsn(INVOKEVIRTUAL, className, "$" + name, "()" + fieldDescriptor, false);
        getter.visitInsn(Util.returnOpcode(fieldDescriptor));

        getter.visitMaxs(0, 0);
        getter.visitEnd();
    }

    @Override
    public void visitEnd()
    {
        classWriter.visitEnd();
    }

    public byte[] toByteArray()
    {
        return classWriter.toByteArray();
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String internalClassName)
    {
        className = internalClassName;
    }

}
