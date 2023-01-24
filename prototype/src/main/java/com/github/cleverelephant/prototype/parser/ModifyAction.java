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

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Executes child actions is the scope of a child property.
 *
 * @author Benjamin Wied
 */
public class ModifyAction extends KeyAction
{
    private final Action[] subActions;

    /**
     * @param key
     *                   scope to execute actions in
     * @param subActions
     *                   to execute
     */
    public ModifyAction(String key, Action[] subActions)
    {
        super(key);
        this.subActions = Arrays.copyOf(subActions, subActions.length);
    }

    @Override
    public void apply(PrototypeContext context, JsonNode parentNode)
    {
        if (!parentNode.isObject())
            reportNotObject(parentNode);

        ObjectNode parent = (ObjectNode) parentNode;

        if (!parent.has(key))
            reportNotDefined(key, parent);

        JsonNode child = parent.get(key);
        for (Action action : subActions)
            action.apply(context, child);
    }

}
