package com.github.cleverelephant.prototype;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface DefaultProtoBuilder
{
    Class<? extends PrototypeBuilder<?, ?>> value();
}
