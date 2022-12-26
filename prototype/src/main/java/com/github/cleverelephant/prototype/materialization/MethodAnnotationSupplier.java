package com.github.cleverelephant.prototype.materialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface MethodAnnotationSupplier
{
    MethodAnnotationSupplier NO_ANNOTATIONS = new MethodAnnotationSupplier() {

        @Override
        public MyAnnotation[] forParameter(String methodName, int paramIndex, Class<?> paramTypes)
        {
            return new MyAnnotation[0];
        }

        @Override
        public MyAnnotation[] forMethod(String methodName, Class<?> returnType, Class<?>... paramTypes)
        {
            return new MyAnnotation[0];
        }
    };

    MyAnnotation[] forMethod(String methodName, Class<?> returnType, Class<?>... paramTypes);
    MyAnnotation[] forParameter(String methodName, int paramIndex, Class<?> paramTypes);

    static MethodAnnotationSupplier annotateMethod(Annotation... annotations)
    {
        MyAnnotation[] myAnnotations = new MyAnnotation[annotations.length];
        for (int i = 0; i < annotations.length; i++)
            myAnnotations[i] = new MyAnnotation(annotations[i]);

        return new MethodAnnotationSupplier() {

            @Override
            public MyAnnotation[] forParameter(String methodName, int paramIndex, Class<?> paramTypes)
            {
                return new MyAnnotation[0];
            }

            @Override
            public MyAnnotation[] forMethod(String methodName, Class<?> returnType, Class<?>... paramTypes)
            {
                return myAnnotations;
            }
        };
    }

    static MethodAnnotationSupplier annotateMethod(Annotated annotated)
    {
        try {
            Method method = annotated.getClass().getMethod("annotations");
            return annotateMethod(method.getAnnotations());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new UnsupportedOperationException("failed to retrieve annotations", e);
        }
    }
}
