package com.github.cleverelephant.prototype;

public interface PrototypeBuilder<T, P extends Prototype<? extends T>>
{
    T build(P proto, String arg);
}
