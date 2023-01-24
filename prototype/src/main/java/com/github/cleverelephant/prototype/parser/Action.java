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
import com.github.cleverelephant.prototype.PrototypeContext;
import com.github.cleverelephant.prototype.PrototypeException;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action defined in an prototype definition. It changes json data of the generated prototype.
 *
 * @author Benjamin Wied
 *
 * @see    DefinitionDeserializer
 * @see    PrototypeDefinition
 */
public abstract class Action
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Action.class);

    /**
     * Reports that the given node is not an object.
     *
     * @param parentNode
     *                   to report
     */
    protected static void reportNotObject(JsonNode parentNode)
    {
        LOGGER.atError().addMarker(Prototype.LOG_MARKER).addKeyValue("parentNode", parentNode)
                .log("parentNode of AddAction must be an ObjectNode");
        throw new IllegalArgumentException("parentNode of AddAction must be an object");
    }

    /**
     * Reports that a node with the given key is already defined as a child of parentNode.
     *
     * @param key
     *                   key of node to report
     * @param parentNode
     *                   parentNode
     */
    protected static void reportAlreadyDefined(String key, JsonNode parentNode)
    {
        LOGGER.atError().addMarker(Prototype.LOG_MARKER).addKeyValue("key", key).addKeyValue("parentNode", parentNode)
                .log("key {} is alreay defined in parentNode", key);
        throw new PrototypeException("key " + key + " is already defined in parentNode");
    }

    /**
     * Reports that no no with the given key is defined a a child of parentNode.
     *
     * @param key
     *                   key of node to report
     * @param parentNode
     *                   parentNode
     */
    protected static void reportNotDefined(String key, JsonNode parentNode)
    {
        LOGGER.atError().addMarker(Prototype.LOG_MARKER).addKeyValue("key", key).addKeyValue("parentNode", parentNode)
                .log("key {} is not defined in parentNode", key);
        throw new PrototypeException("key " + key + " is already defined in parentNode");
    }

    /**
     * Modifies the given data according to the rules defined by this action.
     *
     * @param context
     *                   prototype context to respect, may or may not have an effect
     * @param parentNode
     *                   is modified directly (rather than cloning it first)
     */
    public abstract void apply(PrototypeContext context, JsonNode parentNode);

}
