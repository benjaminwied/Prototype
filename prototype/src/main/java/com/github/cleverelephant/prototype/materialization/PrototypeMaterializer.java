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
package com.github.cleverelephant.prototype.materialization;

import com.github.cleverelephant.prototype.Prototype;
import com.github.cleverelephant.prototype.PrototypeException;

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

/**
 * An {@code AbstractTypeResolver} that resolves Prototypes using byte code generation.
 *
 * @author Benjamin Wied
 *
 * @see    MaterializingClassVisitor
 */
public class PrototypeMaterializer extends AbstractTypeResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeMaterializer.class);

    private final Map<Class<?>, Class<?>> generatedClasses;
    private MyClassLoader classLoader;

    /**
     * Constructs a new PrototypeMaterializer.
     */
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
        if (generatedClasses.containsKey(prototype))
            return generatedClasses.get(prototype);

        synchronized (this) {
            try {
                if (generatedClasses.containsKey(prototype))
                    return generatedClasses.get(prototype);

                Class<?> materialized = materializePrototype(prototype.getName());
                generatedClasses.put(prototype, materialized);
                return materialized;
            } catch (RuntimeException | IOException e) {
                LOGGER.error(Prototype.LOG_MARKER, "failed to materialize prototype class {}", prototype, e);
                throw new PrototypeException(e);
            }
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
