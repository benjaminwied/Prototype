package com.github.cleverelephant.prototype.materialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class ClassGenerator
{
    private final String internalClassName;
    private final String internalSuperClassName;

    private final String[] names;
    private final Class<?>[] types;
    private final Method[] methods;

    public ClassGenerator(String className, Class<?> superClass, Method[] methods)
    {
        internalClassName = internalName(className);
        internalSuperClassName = internalName(superClass.getName());

        this.methods = methods;

        names = new String[methods.length];
        types = new Class<?>[methods.length];
        for (int i = 0; i < methods.length; i++) {
            names[i] = methods[i].getName();
            types[i] = methods[i].getReturnType();
        }
    }

    public byte[] generateClassData()
    {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classWriter.visit(
                V1_8, ACC_PUBLIC, internalName(internalClassName), null, "java/lang/Object",
                new String[] { internalSuperClassName }
        );

        for (int i = 0; i < names.length; i++)
            generateProperty(classWriter, names[i], types[i], methods[i]);

        generateConstructor(classWriter);

        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void generateConstructor(ClassWriter classWriter)
    {
        StringBuilder methodDescription = new StringBuilder("(");
        for (Class<?> type : types)
            methodDescription.append(type.descriptorString());
        methodDescription.append(")V");

        MethodVisitor visitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", methodDescription.toString(), null, null);

        for (int i = 0; i < names.length; i++) {
            visitor.visitParameter(names[i], 0);
            addPropertyAnnotation(visitor, i, names[i]);

            final int idx = i;
            copyAnnotations(methods[i], desc -> visitor.visitParameterAnnotation(idx, desc, true));
        }

        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        for (int i = 0; i < names.length; i++) {
            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitVarInsn(loadOpcode(types[i].descriptorString()), i + 1);
            visitor.visitFieldInsn(PUTFIELD, internalClassName, names[i], types[i].descriptorString());
        }

        visitor.visitInsn(RETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    public void addPropertyAnnotation(MethodVisitor methodVisitor, int parameterIndex, String parameterName)
    {
        /* @JsonProperty annotation already present on method; will be copied */
        if (methods[parameterIndex].isAnnotationPresent(JsonProperty.class))
            return;

        AnnotationVisitor visitor = methodVisitor
                .visitParameterAnnotation(parameterIndex, JsonProperty.class.descriptorString(), true);

        visitor.visit("value", parameterName);
        visitor.visitEnd();
    }

    private void generateProperty(ClassWriter classWriter, String name, Class<?> type, Method method)
    {
        String typeDescriptor = type.descriptorString();

        classWriter.visitField(ACC_PRIVATE, name, typeDescriptor, null, null).visitEnd();

        MethodVisitor visitor = classWriter.visitMethod(ACC_PUBLIC, name, "()" + typeDescriptor, null, null);
        copyAnnotations(method, desc -> visitor.visitAnnotation(desc, true));
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internalClassName, name, typeDescriptor);
        visitor.visitInsn(returnOpcode(typeDescriptor));
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private void copyAnnotations(Method source, Function<String, AnnotationVisitor> visitorGenerator)
    {
        try {
            for (Annotation annotation : source.getDeclaredAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();

                AnnotationVisitor visitor = visitorGenerator.apply(annotationType.descriptorString());

                for (Method method : annotationType.getDeclaredMethods()) {
                    Object value = method.invoke(annotation);
                    if (value instanceof Enum)
                        visitor.visitEnum(
                                method.getName(), value.getClass().descriptorString(), ((Enum<?>) value).name()
                        );
                    else
                        visitor.visit(method.getName(), value);
                }
                visitor.visitEnd();
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private int returnOpcode(String descriptor)
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

    private int loadOpcode(String descriptor)
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

    private final String internalName(String name)
    {
        return name.replace('.', '/');
    }

}
