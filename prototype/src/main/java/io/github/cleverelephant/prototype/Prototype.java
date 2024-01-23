/**
 * MIT License
 *
 * Copyright (c) 2023 Benjamin Wied
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
package io.github.cleverelephant.prototype;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Defines a prototype, which can later be build into a concrete type.
 *
 * @author     Benjamin Wied
 *
 * @param  <T>
 *             type which is represented by this Prototype
 *
 * @see        PrototypeManager
 */
public abstract class Prototype<T>
{
    /**
     * Public LOG_MARKER that is used to log events.
     */
    public static final Marker LOG_MARKER = MarkerFactory.getMarker("prototype");

    /**
     * This is the identifier of this prototype. A prototype can be obtained by using its name in
     * {@link PrototypeManager#getPrototype(String)}.
     */
    public String name;

    /**
     * Builds this prototype into a concrete type.
     *
     * @return the type built
     */
    public abstract T build();
}
