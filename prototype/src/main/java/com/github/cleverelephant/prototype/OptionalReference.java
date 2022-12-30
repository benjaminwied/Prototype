package com.github.cleverelephant.prototype;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that a {@code PrototypeReference} is optional, e.g is nullable.
 *
 * @author Benjamin Wied
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface OptionalReference
{

}
