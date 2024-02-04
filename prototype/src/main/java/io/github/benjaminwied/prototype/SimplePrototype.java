/**
 * MIT License
 *
 * Copyright (c) 2024 Benjamin Wied
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
