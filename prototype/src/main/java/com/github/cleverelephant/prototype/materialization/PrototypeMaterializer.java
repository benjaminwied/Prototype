package com.github.cleverelephant.prototype.materialization;

import com.github.cleverelephant.prototype.Prototype;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.AbstractTypeResolver;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;

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
        return config.constructType(getMaterializedPrototype(type));
    }

    private Class<?> getMaterializedPrototype(JavaType type)
    {
        try {
            if (generatedClasses.containsKey(type.getRawClass()))
                return generatedClasses.get(type.getRawClass());

            Class<?> materialized = materializePrototype(type);
            generatedClasses.put(type.getRawClass(), materialized);
            return materialized;
        } catch (Exception e) {
            LOGGER.error(Prototype.LOG_MARKER, "failed to materialize prototype class {}", type, e);
            throw new UnsupportedOperationException(e);
        }
    }

    private Class<?> materializePrototype(JavaType type)
    {
        Class<?> prototypeClass = type.getRawClass();

        List<Method> properties = findAbstractProperties(prototypeClass);

        String className = "com.github.cleverelephant.materialization.generated." + prototypeClass.getSimpleName();
        byte[] data = new ClassGenerator(className, prototypeClass, properties.toArray(Method[]::new))
                .generateClassData();
        return classLoader.loadAndResolve(className, data);
    }

    private List<Method> findAbstractProperties(Class<?> clazz)
    {
        List<Method> methods = new LinkedList<>();

        for (Method method : clazz.getMethods()) {
            if (method.isBridge() || method.isSynthetic() || method.isDefault() || method.getParameterCount() != 0)
                continue;

            methods.add(method);
        }

        return methods;
    }

    private boolean isPrototype(JavaType type)
    {
        return type.isTypeOrSubTypeOf(Prototype.class) && type.getRawClass().isInterface();
    }

    private class MyClassLoader extends ClassLoader
    {
        public Class<?> loadAndResolve(String name, byte[] data)
        {
            try {
                Class<?> impl = defineClass(name, data, 0, data.length);
                resolveClass(impl);
                return impl;
            } catch (LinkageError e) {
                throw new IllegalArgumentException("Failed to load class '" + name + "': " + e.getMessage(), e);
            }
        }
    }
}
