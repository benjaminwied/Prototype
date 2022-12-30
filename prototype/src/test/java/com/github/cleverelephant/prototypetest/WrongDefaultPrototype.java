package com.github.cleverelephant.prototypetest;

import com.github.cleverelephant.prototype.Prototype;

@SuppressWarnings("javadoc")
public interface WrongDefaultPrototype extends Prototype<String>
{
    String value();

    default int $value()
    {
        return 0;
    }

    default String $default()
    {
        return "def";
    }
}
