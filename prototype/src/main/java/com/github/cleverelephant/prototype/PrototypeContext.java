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

import com.github.cleverelephant.prototype.parser.Action;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Context in which prototypes are processed. Some {@link Action actions} may change behavior depending on the currently
 * active context.
 *
 * @author Benjamin Wied
 *
 * @see    PrototypeRegistry#activateContext(PrototypeContext)
 */
public interface PrototypeContext
{
    /**
     * Default context, containing no keys. It will be active by default.
     */
    PrototypeContext DEFAULT = of();

    /**
     * Checks if this context contains a key with the specified name.
     *
     * @param  key
     *             to check
     *
     * @return     true if and only if this context contains a key with the specified name, false otherwise
     */
    boolean has(String key);

    /**
     * Creates a {@code PrototypeContext} that contains exactly the given keys.
     *
     * @param  keys
     *              will be contained by the returned context
     *
     * @return      a {@code PrototypeContext} that contains exactly the given keys
     */
    static PrototypeContext of(String... keys)
    {
        return new DefaultPrototypeContext(new HashSet<>(Arrays.asList(keys)));
    }

    /**
     * <b>Do not instantiate directly, use {@link PrototypeContext#of(String...)}</b>
     *
     * @author Benjamin Wied
     */
    final class DefaultPrototypeContext implements PrototypeContext
    {
        private final Set<String> keys;

        private DefaultPrototypeContext(Set<String> keys)
        {
            this.keys = keys;
        }

        @Override
        public boolean has(String key)
        {
            return keys.contains(key);
        }
    }
}
