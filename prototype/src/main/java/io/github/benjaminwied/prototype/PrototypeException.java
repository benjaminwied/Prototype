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
package io.github.benjaminwied.prototype;

/**
 * Exception thrown by most prototype related-methods.
 *
 * @author Benjamin Wied
 */
public class PrototypeException extends RuntimeException
{

    /**
     * Constructs a new {@code PrototypeException} with no detail message and cause.
     */
    public PrototypeException()
    {
    }

    /**
     * Constructs a new {@code PrototypeException} with the given detail message and cause.
     *
     * @param message
     *                detail message
     * @param cause
     *                exception cause
     */
    public PrototypeException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a new {@code PrototypeException} with the given detail message and no cause.
     *
     * @param message
     *                detail message
     */
    public PrototypeException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new {@code PrototypeException} with no detail message and the given cause.
     *
     * @param cause
     *              exception cause
     */
    public PrototypeException(Throwable cause)
    {
        super(cause);
    }

}
