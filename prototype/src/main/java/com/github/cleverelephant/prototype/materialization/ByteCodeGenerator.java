package com.github.cleverelephant.prototype.materialization;

import java.util.Map.Entry;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.V1_8;

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
        addMethod("<init>", void.class, types, annotationSupplier, generator -> {
            generator.loadSelf();
            generator.visitor().visitMethodInsn(
                    INVOKESPECIAL, internalSuperClassName, "<init>", Util.methodDescriptor(void.class), false
            );

            for (int i = 0; i < names.length; i++)
                generator.paramToField(names[i], i);
            generator.returnVoid();
        });
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

    public void addMethod(
            String methodName, Class<?> returnType, Class<?>[] paramTypes, Consumer<InstructionGenerator> generator
    )
    {
        addMethod(methodName, returnType, paramTypes, MethodAnnotationSupplier.NO_ANNOTATIONS, generator);
    }

    public void addMethod(
            String methodName, Class<?> returnType, Class<?>[] paramTypes, MethodAnnotationSupplier annotationSupplier,
            Consumer<InstructionGenerator> generator
    )
    {
        MethodVisitor visitor = classWriter
                .visitMethod(ACC_PUBLIC, methodName, Util.methodDescriptor(returnType, paramTypes), null, null);

        for (MyAnnotation annotation : annotationSupplier.forMethod(methodName, void.class))
            addAnnotation(visitor.visitAnnotation(annotation.getAnnotationType().descriptorString(), true), annotation);
        for (int i = 0; i < paramTypes.length; i++)
            for (MyAnnotation annotation : annotationSupplier.forParameter(methodName, i, paramTypes[i]))
                addAnnotation(
                        visitor.visitParameterAnnotation(0, annotation.getAnnotationType().descriptorString(), true),
                        annotation
                );

        generator.accept(new InstructionGenerator(visitor, internalClassName, returnType, paramTypes));

        visitor.visitMaxs(0, 0);
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
        addMethod(methodName, type, new Class<?>[0], annotationSupplier, generator -> {
            generator.loadField(fieldName, type);
            generator.returnType(type);
        });
    }

    public void addSetter(String methodName, String fieldName, Class<?> type)
    {
        addSetter(methodName, fieldName, type, MethodAnnotationSupplier.NO_ANNOTATIONS);
    }

    public void addSetter(
            String methodName, String fieldName, Class<?> type, MethodAnnotationSupplier annotationSupplier
    )
    {
        addMethod(methodName, void.class, new Class<?>[] { type }, annotationSupplier, generator -> {
            generator.paramToField(fieldName, 0);
            generator.returnVoid();
        });
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
}
