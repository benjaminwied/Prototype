package com.github.cleverelephant.prototype.materialization;

public interface FieldAnnotationSupplier
{
    FieldAnnotationSupplier NO_ANNOTATIONS = (fieldName, fieldType) -> new MyAnnotation[0];

    MyAnnotation[] forField(String fieldName, Class<?> fieldType);
}
