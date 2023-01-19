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

import com.github.cleverelephant.prototype.Prototype;
import com.github.cleverelephant.prototype.PrototypeException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds the specified property to the data, failing if an property with the same name already exists.
 *
 * @author Benjamin Wied
 */
public class AddAction extends Action
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddAction.class);

    private final JsonNode data;

    /**
     * Creates a new AddAction using the specified key and data
     *
     * @param key
     *             property name
     * @param data
     *             property value
     */
    public AddAction(String key, JsonNode data)
    {
        super(key);
        this.data = data;
    }

    @Override
    public void apply(JsonNode parentNode)
    {
        if (!parentNode.isObject())
            reportNotObject(parentNode);

        ObjectNode objectNode = (ObjectNode) parentNode;
        if (objectNode.has(key))
            reportAlreadyDefined(parentNode);

        objectNode.set(key, data.deepCopy());
    }

    private void reportAlreadyDefined(JsonNode parentNode)
    {
        LOGGER.atError().addMarker(Prototype.LOG_MARKER).addKeyValue("key", key).addKeyValue("data", data)
                .addKeyValue("parentNode", parentNode).log("key {} is alreay defined in parentNode", key);
        throw new PrototypeException("key " + key + " is already defined in parentNode");
    }

    private void reportNotObject(JsonNode parentNode)
    {
        LOGGER.atError().addMarker(Prototype.LOG_MARKER).addKeyValue("key", key).addKeyValue("data", data)
                .addKeyValue("parentNode", parentNode).log("parentNode of AddAction must be an ObjectNode");
        throw new IllegalArgumentException("parentNode of AddAction must be an object");
    }

}
