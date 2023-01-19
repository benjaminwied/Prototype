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
package com.github.cleverelephant.prototype.parser;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An action defined in an prototype definition. It changes to json data of the generated prototype.
 *
 * @author Benjamin Wied
 *
 * @see    DefinitionDeserializer
 * @see    PrototypeDefinition
 */
public abstract class Action
{
    /**
     * The key (or property name) this action corresponds to. It is the class name of the generated prototype if used as
     * a top-level action.
     */
    protected final String key;

    /**
     * Creates a new Action using the specified key.
     *
     * @param key
     *            key (or property name)
     */
    protected Action(String key)
    {
        this.key = key;
    }

    /**
     * Modifies the given data according to the rules defined by this action.
     *
     * @param data
     *             is modified directly (rather than cloning it first)
     */
    public abstract void apply(JsonNode data);
}
