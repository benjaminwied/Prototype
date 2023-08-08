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
package com.github.cleverelephant.prototype;

import java.util.Collections;
import java.util.Map;

/**
 * Context in which prototypes are processed.
 *
 * @author Benjamin Wied
 */
public final class PrototypeContext
{
    private static Map<String, Object> currentContext = Collections.emptyMap();
    private static boolean isContextLive = true;

    /**
     * Activates a context to generate prototypes. <em>Note: for this to take effect, all prototypes need to be
     * reloaded.</em>
     *
     * @param context
     *                to activate
     */
    public static void activateConent(Map<String, Object> context)
    {
        currentContext = Collections.unmodifiableMap(context);
        isContextLive = false;
    }

    /**
     * Returns the current context as an immutable Map.
     *
     * @return the current context as an immutable Map.
     */
    public static Map<String, Object> getCurrentContext()
    {
        return currentContext;
    }

    /**
     * Returns true if the context is live, false otherwise. To make an active context live, all prototypes need to be
     * reloaded.
     *
     * @return true if the context is live
     */
    public static boolean isContextLive()
    {
        return isContextLive;
    }

    /**
     * Sets the live flag of the context.
     */
    static void makeContextLive()
    {
        isContextLive = true;
    }

    private PrototypeContext()
    {
        /* do nothing */
    }
}
