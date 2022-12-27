package com.github.cleverelephant.prototype.materialization;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public class ClassGenerator
{
    private final class SetterAnnotations implements MethodAnnotationSupplier
    {
        private final int index;
        private MethodAnnotationSupplier parent;

        private SetterAnnotations(int index)
        {
            this.index = index;
            parent = MethodAnnotationSupplier.annotateMethod(methods[index].getAnnotations());
        }

        @Override
        public MyAnnotation[] forParameter(String methodName, int paramIndex, Class<?> paramTypes)
        {
            return new MyAnnotation[0];
        }

        @Override
        public MyAnnotation[] forMethod(String methodName, Class<?> returnType, Class<?>... paramTypes)
        {
            Set<MyAnnotation> annotations = new HashSet<>(
                    Arrays.asList(parent.forMethod(methodName, returnType, paramTypes))
            );

            if (annotations.stream().noneMatch(JsonProperty.class::isInstance))
                annotations.add(new MyAnnotation(JsonProperty.class).addMapping("value", names[index]));
            else {
                MyAnnotation a = annotations.stream().filter(JsonProperty.class::isInstance).findFirst().orElseThrow();

                if (a.isDefaulMapping("value")) {
                    MyAnnotation annotation = new MyAnnotation(a);
                    annotation.addMapping("value", names[index]);

                    annotations.remove(a);
                    annotations.add(annotation);
                }
            }

            return annotations.toArray(MyAnnotation[]::new);
        }
    }

    private final String className;
    private final String superClassName;

    private final String[] names;
    private final Class<?>[] types;
    private final Method[] methods;

    public ClassGenerator(String className, Class<?> superClass, Method[] methods)
    {
        this.className = className;
        superClassName = superClass.getName();

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
        ByteCodeGenerator byteCodeGenerator = new ByteCodeGenerator(
                className, Object.class.getName(), new String[] { superClassName }
        );

        for (int i = 0; i < names.length; i++) {
            final int index = i;

            byteCodeGenerator.addField(names[i], types[i]);
            byteCodeGenerator.addField("$" + names[i], boolean.class);

            byteCodeGenerator.addMethod(names[i], types[i], new Class<?>[0], generator -> {
                generator.loadField("$" + names[index], boolean.class);
                Label label = new Label();
                generator.visitor().visitJumpInsn(Opcodes.IFEQ, label);

                generator.loadField(names[index], types[index]);
                generator.returnType(types[index]);

                generator.visitor().visitLabel(label);
                generator.visitor().visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
                generator.invokeOwnMethod("$" + names[index], types[index]);
                //                generator.invokeVirtual("$" + names[index], types[index]);
                generator.returnType(types[index]);

            });

            byteCodeGenerator.addMethod(
                    names[i], void.class, new Class<?>[] { types[i] }, new SetterAnnotations(index), generator -> {
                        generator.paramToField(names[index], 0);
                        generator.setFieldToConst("$" + names[index], boolean.class, true);
                        generator.returnVoid();
                    }
            );
        }

        byteCodeGenerator.addDefaultConstructor();
        byteCodeGenerator.addConstructor(names, types);

        return byteCodeGenerator.generate();
    }

}
