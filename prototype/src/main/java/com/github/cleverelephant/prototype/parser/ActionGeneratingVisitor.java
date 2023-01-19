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

import com.github.cleverelephant.prototype.parser.antlr.PrototypeBaseVisitor;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.AddActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ArrayContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.KeyValueContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ObjectContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.PrototypeContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ValueContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Visits a {@link PrototypeContext} and maps it to a corresponding action.
 *
 * @author Benjamin Wied
 */
class ActionGeneratingVisitor extends PrototypeBaseVisitor<Action>
{
    @Override
    public Action visitAddAction(AddActionContext ctxt)
    {
        KeyValueContext keyValueContext = ctxt.keyValue();
        String key = string(keyValueContext.key().getText());
        JsonNode node = jsonValue(keyValueContext.value());

        return new AddAction(key, node);
    }

    private static ArrayNode array(ArrayContext array)
    {
        ArrayNode node = new ArrayNode(JsonNodeFactory.instance);
        for (ValueContext valueContext : array.getRuleContexts(ValueContext.class))
            node.add(jsonValue(valueContext));
        return node;
    }

    private static ObjectNode object(ObjectContext object)
    {
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
        for (KeyValueContext keyValueContext : object.getRuleContexts(KeyValueContext.class)) {
            String key = string(keyValueContext.key().getText());
            JsonNode child = jsonValue(keyValueContext.value());

            node.set(key, child);
        }
        return node;
    }

    private static JsonNode jsonValue(ValueContext context)
    {
        if (context.object() != null)
            return object(context.object());
        if (context.array() != null)
            return array(context.array());
        if (context.STRINGLITERAL() != null)
            return TextNode.valueOf(string(context.getText()));
        if (context.NUMBER() != null) {
            String text = context.getText();
            try {
                return IntNode.valueOf(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                /* May be a floating-point number */
                return DoubleNode.valueOf(Double.parseDouble(text));
            }
        }
        if (context.BOOLEAN() != null)
            return BooleanNode.valueOf(Boolean.parseBoolean(context.getText()));

        throw new IllegalArgumentException("unknown context type: " + context);
    }

    private static String string(String string)
    {
        return string.substring(1, string.length() - 1);
    }

    @Override
    protected Action aggregateResult(Action aggregate, Action nextResult)
    {
        if (nextResult != null)
            return nextResult;
        return aggregate;
    }

}
