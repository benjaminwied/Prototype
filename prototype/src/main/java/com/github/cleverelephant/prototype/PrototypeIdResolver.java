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

import com.github.cleverelephant.prototype.materialization.PrototypeMaterializer;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

/**
 * Uses the class name as an id, generates prototype implementing classes on-the-fly.
 *
 * @author Benjamin Wied
 */
class PrototypeIdResolver extends TypeIdResolverBase
{
    private static final PrototypeMaterializer MATERIALIZER = new PrototypeMaterializer();

    @Override
    public String idFromValue(Object value)
    {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType)
    {
        throw new UnsupportedOperationException("serialization of prototypes not supported");
    }

    @Override
    public Id getMechanism()
    {
        return Id.CUSTOM;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException
    {
        try {
            Class<?> protoClass = Class.forName(id);
            Class<?> materialized = MATERIALIZER.getPrototypeImplemetation(protoClass);
            return context.constructType(materialized);
        } catch (ClassNotFoundException e) {
            return ((DeserializationContext) context).handleUnknownTypeId(_baseType, id, this, "no such class found");
        }
    }

}
