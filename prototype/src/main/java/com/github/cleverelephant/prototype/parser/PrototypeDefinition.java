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

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Maintains an action-chain and creates the final json-data for prototype definitions.<br>
 * <br>
 * Objects of this class are immutable.
 *
 * @author Benjamin Wied
 */
public class PrototypeDefinition
{
    private final Class<? extends Prototype<?>> prototypeClass;
    private final Action[] actions;

    /**
     * Creates a new PrototypeDefinition for a prototype with the given name.
     *
     * @param prototypeClass
     *                       prototype class
     * @param actions
     *                       prototype actions
     */
    public PrototypeDefinition(Class<? extends Prototype<?>> prototypeClass, Action... actions)
    {
        this.prototypeClass = prototypeClass;
        this.actions = Arrays.copyOf(actions, actions.length);
    }

    /**
     * Creates a PrototypeDefinition, that, when applied, will first apply this definition and than the method
     * parameter.
     *
     * @param  other
     *                            to apply after this {@code this} definition
     *
     * @return                    a new PrototypeDefinition
     *
     * @throws PrototypeException
     *                            if the prototype classes represented by {@code this} and {@code other} are not
     *                            compatible.
     */
    public PrototypeDefinition merge(PrototypeDefinition other)
    {
        Class<? extends Prototype<?>> pc = prototypeClass;
        if (prototypeClass == null)
            pc = other.prototypeClass;
        else if (other.prototypeClass != null) {
            /* Compatibility currently only means: is subclass */
            if (!pc.isAssignableFrom(other.prototypeClass))
                throw new PrototypeException(
                        "Canno merge PrototypeDefinitions: incompatible classes (" + prototypeClass + ","
                                + other.prototypeClass + ")"
                );
            pc = other.prototypeClass;
        }

        Action[] a = Arrays.copyOf(actions, actions.length + other.actions.length);
        System.arraycopy(other.actions, 0, a, actions.length, other.actions.length);

        return new PrototypeDefinition(pc, a);
    }

    /**
     * Returns the data represented by this action, including the name of the prototype class.
     *
     * @param  context
     *                 active context
     *
     * @return         generated data
     */
    public JsonNode getData(PrototypeContext context)
    {
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        apply(context, data);

        ObjectNode wrapped = new ObjectNode(JsonNodeFactory.instance);
        wrapped.set(prototypeClass.getName(), data);
        return wrapped;
    }

    /**
     * Applies all actions contained in this definition to the given node.
     *
     * @param context
     *                active context
     * @param data
     *                to modify
     */
    public void apply(PrototypeContext context, ObjectNode data)
    {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(data, "data must not be null");

        for (Action action : actions)
            action.apply(context, data);
    }

    /**
     * @return the prototype class represented by this definition, or {@code null} if no class was specified
     */
    public Class<?> getPrototypeClass()
    {
        return prototypeClass;
    }
}
