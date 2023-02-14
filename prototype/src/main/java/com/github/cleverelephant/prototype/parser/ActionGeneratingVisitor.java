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
import com.github.cleverelephant.prototype.parser.antlr.PrototypeBaseVisitor;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.AddActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ApplyFromActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ArrayContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ConditionalActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ContentContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.KeyValueContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ModifyActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ObjectContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.PrototypeContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.RemoveActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.RenameActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ReplaceActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.SetActionContext;
import com.github.cleverelephant.prototype.parser.antlr.PrototypeParser.ValueContext;

import java.util.function.BiFunction;

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
class ActionGeneratingVisitor extends PrototypeBaseVisitor<Object>
{

    @SuppressWarnings("unchecked")
    @Override
    public PrototypeDefinition visitPrototype(PrototypeContext ctxt)
    {
        Class<? extends Prototype<?>> clazz = null;
        if (ctxt.header() != null && ctxt.header().classDefinition() != null)
            try {
                clazz = (Class<? extends Prototype<?>>) Class
                        .forName(string(ctxt.header().classDefinition().STRINGLITERAL().toString()))
                        .asSubclass(Prototype.class);
            } catch (ClassNotFoundException e) {
                throw new PrototypeException(e);
            }

        Action[] actions = visitContent(ctxt.content());
        return new PrototypeDefinition(clazz, actions);
    }

    @Override
    public Object visitConditionalAction(ConditionalActionContext ctxt)
    {
        String condition = string(ctxt.STRINGLITERAL().getText());
        Action[] actions = ctxt.action().stream().map(this::visitAction).toArray(Action[]::new);

        return new ConditionalAction(condition, actions);
    }

    @Override
    public Action[] visitContent(ContentContext ctxt)
    {
        return ctxt.action().stream().map(this::visitAction).toArray(Action[]::new);
    }

    @Override
    public Object visitApplyFromAction(ApplyFromActionContext ctxt)
    {
        String source = string(ctxt.STRINGLITERAL().getText());
        return new ApplyFromAction(source);
    }

    @Override
    public Action visitModifyAction(ModifyActionContext ctxt)
    {
        String source = string(ctxt.key().getText());
        Action[] action = ctxt.action().stream().map(this::visitAction).toArray(Action[]::new);
        return new ModifyAction(source, action);
    }

    @Override
    public Action visitAddAction(AddActionContext ctxt)
    {
        return visitSetLikeAction(ctxt.keyValue(), AddAction::new);
    }

    @Override
    public Action visitRenameAction(RenameActionContext ctxt)
    {
        String source = string(ctxt.key(0).getText());
        String target = string(ctxt.key(1).getText());

        return new RenameAction(source, target);
    }

    @Override
    public Action visitRemoveAction(RemoveActionContext ctxt)
    {
        String key = string(ctxt.key().getText());
        return new RemoveAction(key);
    }

    @Override
    public Action visitSetAction(SetActionContext ctxt)
    {
        return visitSetLikeAction(ctxt.keyValue(), SetAction::new);
    }

    @Override
    public Action visitReplaceAction(ReplaceActionContext ctxt)
    {
        return visitSetLikeAction(ctxt.keyValue(), ReplaceAction::new);
    }

    private static Action visitSetLikeAction(KeyValueContext ctxt, BiFunction<String, JsonNode, Action> generater)
    {
        String key = string(ctxt.key().getText());
        JsonNode node = jsonValue(ctxt.value());

        return generater.apply(key, node);
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
    protected Object aggregateResult(Object aggregate, Object nextResult)
    {
        if (nextResult != null)
            return nextResult;
        return aggregate;
    }

    @Override
    public Action visitAction(ActionContext ctx)
    {
        return (Action) super.visitAction(ctx);
    }

}
