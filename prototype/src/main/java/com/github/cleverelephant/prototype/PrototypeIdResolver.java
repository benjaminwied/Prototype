package com.github.cleverelephant.prototype;

import com.github.cleverelephant.prototype.materialization.PrototypeMaterializer;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

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
