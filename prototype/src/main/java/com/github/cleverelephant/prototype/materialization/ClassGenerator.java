package com.github.cleverelephant.prototype.materialization;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClassGenerator
{
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
            byteCodeGenerator.addField(names[i], types[i]);
            byteCodeGenerator.addGetter(names[i], names[i], types[i]);

            final int index = i;
            byteCodeGenerator.addSetter(names[i], names[i], types[i], new MethodAnnotationSupplier() {
                private MethodAnnotationSupplier parent = MethodAnnotationSupplier
                        .annotateMethod(methods[index].getAnnotations());

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
                        MyAnnotation a = annotations.stream().filter(JsonProperty.class::isInstance).findFirst()
                                .orElseThrow();

                        if (a.isDefaulMapping("value")) {
                            MyAnnotation annotation = new MyAnnotation(a);
                            annotation.addMapping("value", names[index]);

                            annotations.remove(a);
                            annotations.add(annotation);
                        }
                    }

                    return annotations.toArray(MyAnnotation[]::new);
                }
            });
        }

        byteCodeGenerator.addDefaultConstructor();
        byteCodeGenerator.addConstructor(names, types);

        return byteCodeGenerator.generate();
    }

}
