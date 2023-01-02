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
package com.github.cleverelephant.prototype.materialization;

import static org.objectweb.asm.Opcodes.*;

/**
 * Class generation utilities.
 *
 * @author Benjamin Wied
 */
public final class Util
{
    private Util()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the jvm instruction to load something with the given descriptor.
     *
     * @param  descriptor
     *                    to load
     *
     * @return            jvm instruction
     */
    public static int loadOpcode(String descriptor)
    {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> ILOAD;
            case "F" -> FLOAD;
            case "D" -> DLOAD;
            case "J" -> LLOAD;
            default -> {
                if (!descriptor.startsWith("L") && !descriptor.startsWith("["))
                    throw new IllegalArgumentException("invalid descriptor " + descriptor);
                yield ALOAD;
            }
        };
    }

    /**
     * Returns the jvm instruction to return something with the given descriptor.
     *
     * @param  descriptor
     *                    to return
     *
     * @return            jvm instruction
     */
    public static int returnOpcode(String descriptor)
    {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> IRETURN;
            case "F" -> FRETURN;
            case "D" -> DRETURN;
            case "J" -> LRETURN;
            default -> {
                if (!descriptor.startsWith("L") && !descriptor.startsWith("["))
                    throw new IllegalArgumentException("invalid descriptor " + descriptor);
                yield ARETURN;
            }
        };
    }
}
