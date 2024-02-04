package io.github.benjaminwied.prototype;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simple implementation of the {@link Prototype} interface. Builds prototypes using a builder function.
 *
 * @author     Benjamin Wied
 *
 * @param  <T>
 *             type which is represented by this Prototype
 */
public class SimplePrototype<T> implements Prototype<T>
{
    private final String name;
    private final Supplier<T> builder;

    /**
     * Creates a new SimplePrototype that invokes the builder's {@link Supplier#get() get} method for each
     * {@link #build()} call.
     *
     * @param name
     *                prototype name
     * @param builder
     *                prototype builder
     */
    public SimplePrototype(String name, Supplier<T> builder)
    {
        this.name = name;
        this.builder = builder;
    }

    /**
     * Creates a new SimplePrototype that invokes the builder's {@link Function#apply(Object) apply} method for each
     * {@link #build()} call. The function's argument is the name of the prototype.
     *
     * @param name
     *                prototype name
     * @param builder
     *                prototype builder
     */
    public SimplePrototype(String name, Function<String, T> builder)
    {
        this.name = name;
        this.builder = () -> builder.apply(name);
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public T build()
    {
        return builder.get();
    }

}
