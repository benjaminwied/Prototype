package com.github.cleverelephant.prototype.materialization;

import com.github.cleverelephant.prototype.Prototype;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.AbstractTypeResolver;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;

import org.objectweb.asm.ClassReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrototypeMaterializer extends AbstractTypeResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeMaterializer.class);

    private final Map<Class<?>, Class<?>> generatedClasses;
    private MyClassLoader classLoader;

    public PrototypeMaterializer()
    {
        generatedClasses = new HashMap<>();
        classLoader = new MyClassLoader();
    }

    @Override
    public JavaType resolveAbstractType(DeserializationConfig config, BeanDescription typeDesc)
    {
        JavaType type = typeDesc.getType();
        if (!isPrototype(type))
            return null;
        return config.constructType(getMaterializedPrototype(type.getRawClass()));
    }

    private Class<?> getMaterializedPrototype(Class<?> prototype)
    {
        try {
            if (generatedClasses.containsKey(prototype))
                return generatedClasses.get(prototype);

            Class<?> materialized = materializePrototype(prototype.getName());
            generatedClasses.put(prototype, materialized);
            return materialized;
        } catch (Exception e) {
            LOGGER.error(Prototype.LOG_MARKER, "failed to materialize prototype class {}", prototype, e);
            throw new UnsupportedOperationException(e);
        }
    }

    private Class<?> materializePrototype(String name) throws IOException
    {
        ClassReader classReader = new ClassReader(name);
        MaterializingClassVisitor classVisitor = new MaterializingClassVisitor(this::getMaterializedPrototype);
        classReader.accept(classVisitor, 0);

        byte[] data = classVisitor.toByteArray();
        return classLoader.loadAndResolve(data);
    }

    private static boolean isPrototype(JavaType type)
    {
        return type.isTypeOrSubTypeOf(Prototype.class) && type.getRawClass().isInterface();
    }

    private static class MyClassLoader extends ClassLoader
    {
        public Class<?> loadAndResolve(byte[] data)
        {
            try {
                Class<?> impl = defineClass(null, data, 0, data.length);
                resolveClass(impl);
                return impl;
            } catch (LinkageError e) {
                throw new IllegalArgumentException("Failed to load class: " + e.getMessage(), e);
            }
        }
    }
}
