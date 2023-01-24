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

import com.github.cleverelephant.prototype.PrototypeContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Sets the specified property.
 *
 * @author Benjamin Wied
 */
public class SetAction extends KeyAction
{
    private final JsonNode data;

    /**
     * Creates a new SetAction using the specified key and data
     *
     * @param key
     *             property name
     * @param data
     *             property value
     */
    public SetAction(String key, JsonNode data)
    {
        super(key);
        this.data = data;
    }

    @Override
    public void apply(PrototypeContext context, JsonNode parentNode)
    {
        if (!parentNode.isObject())
            reportNotObject(parentNode);

        ObjectNode objectNode = (ObjectNode) parentNode;
        set(key, objectNode, data.deepCopy());
    }

    /**
     * Sets the specified property, subclasses may override this to perform additional checks.
     *
     * @param key
     *               property name
     * @param parent
     *               property owner
     * @param data
     *               value
     */
    @SuppressWarnings("static-method")
    protected void set(String key, ObjectNode parent, JsonNode data)
    {
        parent.set(key, data);
    }

}
