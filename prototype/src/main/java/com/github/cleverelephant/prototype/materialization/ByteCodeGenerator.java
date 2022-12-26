package com.github.cleverelephant.prototype.materialization;

import java.util.Map.Entry;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class ByteCodeGenerator
{
    private final ClassWriter classWriter;
    private final String internalClassName, internalSuperClassName;

    public ByteCodeGenerator(String className, String superClassName, String[] superInterfaces)
    {
        this(className, superClassName, superInterfaces, ClassAnotationSupplier.NO_ANNOTATIONS);
    }

    public ByteCodeGenerator(
            String className, String superClassName, String[] superInterfaces, ClassAnotationSupplier anotationSupplier
    )
    {
        classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        internalClassName = internalName(className);
        internalSuperClassName = internalName(superClassName);
        String[] internalSuperInterfaces = new String[superInterfaces.length];
        for (int i = 0; i < superInterfaces.length; i++)
            internalSuperInterfaces[i] = internalName(superInterfaces[i]);

        classWriter.visit(V1_8, ACC_PUBLIC, internalClassName, null, internalSuperClassName, internalSuperInterfaces);
        for (MyAnnotation annotation : anotationSupplier.forClass(className))
            addAnnotation(
                    classWriter.visitAnnotation(annotation.getAnnotationType().descriptorString(), true), annotation
            );
    }

    private static void addAnnotation(AnnotationVisitor visitor, MyAnnotation annotation)
    {
        for (Entry<String, Object> entry : annotation.getValues().entrySet()) {
            // TODO visit arrays & nested annotations

            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Enum)
                visitor.visitEnum(name, value.getClass().descriptorString(), ((Enum<?>) value).name());
            else
                visitor.visit(name, value);
        }
        visitor.visitEnd();
    }

    public void addDefaultConstructor()
    {
        addConstructor(new String[0], new Class<?>[0]);
    }

    public void addDefaultConstructor(MethodAnnotationSupplier annotationSupplier)
    {
        addConstructor(new String[0], new Class<?>[0], annotationSupplier);
    }

    public void addConstructor(String[] names, Class<?>[] types)
    {
        addConstructor(names, types, MethodAnnotationSupplier.NO_ANNOTATIONS);
    }

    public void addConstructor(String[] names, Class<?>[] types, MethodAnnotationSupplier annotationSupplier)
    {
        MethodVisitor visitor = classWriter
                .visitMethod(ACC_PUBLIC, "<init>", methodDescriptor(void.class, types), null, null);

        for (MyAnnotation annotation : annotationSupplier.forMethod("<init>", void.class))
            addAnnotation(visitor.visitAnnotation(annotation.getAnnotationType().descriptorString(), true), annotation);
        for (int i = 0; i < names.length; i++)
            for (MyAnnotation annotation : annotationSupplier.forParameter("<init>", i, types[i]))
                addAnnotation(
                        visitor.visitParameterAnnotation(0, annotation.getAnnotationType().descriptorString(), true),
                        annotation
                );

        // TODO
        //        for (int i = 0; i < names.length; i++) {
        //            visitor.visitParameter(names[i], 0);
        //            addPropertyAnnotation(visitor, i, names[i]);
        //
        //            final int idx = i;
        //            copyAnnotations(methods[i], desc -> visitor.visitParameterAnnotation(idx, desc, true));
        //        }

        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitMethodInsn(INVOKESPECIAL, internalSuperClassName, "<init>", methodDescriptor(void.class), false);

        for (int i = 0; i < names.length; i++) {
            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitVarInsn(loadOpcode(types[i].descriptorString()), i + 1);
            visitor.visitFieldInsn(PUTFIELD, internalClassName, names[i], types[i].descriptorString());
        }

        visitor.visitInsn(RETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private static String methodDescriptor(Class<?> returnType, Class<?>... paramTypes)
    {
        StringBuilder methodDescriptor = new StringBuilder("(");
        for (Class<?> type : paramTypes)
            methodDescriptor.append(type.descriptorString());
        methodDescriptor.append(")").append(returnType.descriptorString());
        return methodDescriptor.toString();
    }

    public void addField(String name, Class<?> type)
    {
        addField(name, type, FieldAnnotationSupplier.NO_ANNOTATIONS);
    }

    public void addField(String name, Class<?> type, FieldAnnotationSupplier annotationSupplier)
    {
        FieldVisitor visitor = classWriter.visitField(ACC_PRIVATE, name, type.descriptorString(), null, null);

        for (MyAnnotation annotation : annotationSupplier.forField(name, type))
            addAnnotation(visitor.visitAnnotation(annotation.getAnnotationType().descriptorString(), true), annotation);
        visitor.visitEnd();
    }

    public void addGetter(String methodName, String fieldName, Class<?> type)
    {
        addGetter(methodName, fieldName, type, MethodAnnotationSupplier.NO_ANNOTATIONS);
    }

    public void addGetter(
            String methodName, String fieldName, Class<?> type, MethodAnnotationSupplier annotationSupplier
    )
    {
        String typeDescriptor = type.descriptorString();

        MethodVisitor visitor = classWriter.visitMethod(ACC_PUBLIC, methodName, "()" + typeDescriptor, null, null);

        for (MyAnnotation annotation : annotationSupplier.forMethod(methodName, type))
            addAnnotation(visitor.visitAnnotation(annotation.getAnnotationType().descriptorString(), true), annotation);

        // TODO: copyAnnotations(method, desc -> visitor.visitAnnotation(desc, true));
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internalClassName, fieldName, typeDescriptor);
        visitor.visitInsn(returnOpcode(typeDescriptor));
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    public void addSetter(String methodName, String fieldName, Class<?> type)
    {
        addSetter(methodName, fieldName, type, MethodAnnotationSupplier.NO_ANNOTATIONS);
    }

    public void addSetter(
            String methodName, String fieldName, Class<?> type, MethodAnnotationSupplier annotationSupplier
    )
    {
        String typeDescriptor = type.descriptorString();

        MethodVisitor visitor = classWriter
                .visitMethod(ACC_PUBLIC, methodName, "(" + typeDescriptor + ")V", null, null);

        for (MyAnnotation annotation : annotationSupplier.forMethod(methodName, type))
            addAnnotation(visitor.visitAnnotation(annotation.getAnnotationType().descriptorString(), true), annotation);
        for (MyAnnotation annotation : annotationSupplier.forParameter(methodName, 0, type))
            addAnnotation(
                    visitor.visitParameterAnnotation(0, annotation.getAnnotationType().descriptorString(), true),
                    annotation
            );

        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(loadOpcode(typeDescriptor), 1);
        visitor.visitFieldInsn(PUTFIELD, internalClassName, fieldName, typeDescriptor);

        visitor.visitInsn(RETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    public byte[] generate()
    {
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private static String internalName(String name)
    {
        return name.replace('.', '/');
    }

    private static int returnOpcode(String descriptor)
    {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> IRETURN;
            case "F" -> FRETURN;
            case "D" -> DRETURN;
            case "J" -> LRETURN;
            default -> {
                if (!descriptor.startsWith("L"))
                    throw new IllegalArgumentException("invalid descriptor " + descriptor);
                yield ARETURN;
            }
        };
    }

    private static int loadOpcode(String descriptor)
    {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> ILOAD;
            case "F" -> FLOAD;
            case "D" -> DLOAD;
            case "J" -> LLOAD;
            default -> {
                if (!descriptor.startsWith("L"))
                    throw new IllegalArgumentException("invalid descriptor " + descriptor);
                yield ALOAD;
            }
        };
    }
}
