package com.github.cleverelephant.prototype.materialization;

public interface ClassAnotationSupplier
{
    ClassAnotationSupplier NO_ANNOTATIONS = name -> new MyAnnotation[0];

    MyAnnotation[] forClass(String name);
}
