package com.github.cleverelephant.prototypetest;

import com.github.cleverelephant.prototype.Prototype;

public interface TestPrototype extends Prototype<String>
{
    String a();
    int b();
    boolean c();
    String d();
    String[] array();
    double doubleValue();

    default String $d()
    {
        return "abc";
    }
}
