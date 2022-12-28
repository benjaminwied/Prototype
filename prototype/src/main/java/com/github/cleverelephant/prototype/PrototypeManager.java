/**
 * MIT License
 *
 * Copyright (c) 2022 Benjamin Wied
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("javadoc")
public final class PrototypeManager
{
    private static final Pattern PROTOTYPE_WITH_ARG_PATTERN = Pattern
            .compile("^(?<name>\\w+)(?:\\[(?<arg>\\w+)\\])?$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PROTOTYPE_NAME_PATTERN = Pattern
            .compile("^\\w+(?:/\\w+)*$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeManager.class);

    private static final Map<String, Prototype<?>> PROTOTYPE_MAP = new HashMap<>();
    private static final Map<Class<? extends Prototype<?>>, PrototypeBuilder<?, ?>> BUILDER_MAP = new HashMap<>();

    private PrototypeManager()
    {
        throw new UnsupportedOperationException();
    }

    public static <T, P extends Prototype<? extends T>> void registerBuilder(
            Class<P> prototypeClass, PrototypeBuilder<T, P> builder
    )
    {
        Objects.requireNonNull(prototypeClass, "prototypeClass must not be null");
        Objects.requireNonNull(builder, "builder must not be null");

        PrototypeBuilder<?, ?> old = BUILDER_MAP.put(prototypeClass, builder);

        if (old != null)
            LOGGER.warn(
                    Prototype.LOG_MARKER, "Added builder {} for prototype {}, replacing old builder {}",
                    builder.getClass(), prototypeClass, old.getClass()
            );
        else
            LOGGER.trace(Prototype.LOG_MARKER, "Added builder {} for prototype {}", builder.getClass(), prototypeClass);
    }

    public static void putPrototype(Prototype<?> prototype)
    {
        Objects.requireNonNull(prototype, "prototype must not be null");
        String name = prototype.name();
        checkName(PROTOTYPE_NAME_PATTERN, name);

        synchronized (PrototypeManager.class) {
            Prototype<?> old = PROTOTYPE_MAP.put(name, prototype);

            if (old != null)
                LOGGER.warn(Prototype.LOG_MARKER, "Added prototype '{}' to collection, replacing old value", name);
            else
                LOGGER.trace(Prototype.LOG_MARKER, "Added prototype '{}' to collection", name);
        }
    }

    public static <T, P extends Prototype<T>> Optional<P> getPrototype(Class<T> typeClass, String name)
    {
        Objects.requireNonNull(typeClass, "typeClass must not be null");
        Objects.requireNonNull(name, "name must not be null");

        Matcher matcher = checkName(PROTOTYPE_WITH_ARG_PATTERN, name);
        /* Discard parameters */
        name = matcher.group("name");

        return getPrototypeNoNameCheck(typeClass, name);
    }

    @SuppressWarnings("unchecked")
    private static <T, P extends Prototype<?>> Optional<P> getPrototypeNoNameCheck(Class<T> typeClass, String name)
    {
        Prototype<?> prototype = PROTOTYPE_MAP.get(name);
        if (prototype == null) {
            LOGGER.trace(Prototype.LOG_MARKER, "No prototypes found for name {}", name, typeClass);
            return Optional.empty();
        }

        return Optional.of((P) prototype);
    }

    public static <T> T createType(Class<T> typeClass, String name)
    {
        Objects.requireNonNull(typeClass, "typeClass must not be null");
        Objects.requireNonNull(name, "name must not be null");

        return optionalCreateType(typeClass, name)
                .orElseThrow(() -> new IllegalArgumentException(name + "/" + typeClass));
    }

    public static <T> Optional<T> optionalCreateType(Class<T> typeClass, String name)
    {
        Objects.requireNonNull(typeClass, "typeClass must not be null");
        Objects.requireNonNull(name, "name must not be null");

        Matcher matcher = checkName(PROTOTYPE_WITH_ARG_PATTERN, name);

        Optional<Prototype<T>> prototype = getPrototype(typeClass, matcher.group("name"));
        if (prototype.isEmpty())
            return Optional.empty();
        return Optional.of(createType(prototype.get(), matcher.group("arg")));
    }

    public static <T, P extends Prototype<? extends T>> T createType(P proto)
    {
        return createType(proto, null);
    }

    public static <T, P extends Prototype<? extends T>> T createType(P proto, String arg)
    {
        Objects.requireNonNull(proto, "proto must not be null");

        PrototypeBuilder<T, P> builder = prototypeBuilder(proto.getClass());
        if (builder == null)
            throw new UnsupportedOperationException("no builder found for prototype " + proto);

        return builder.build(proto, arg);
    }

    @SuppressWarnings("unchecked")
    public static <T,
            P extends Prototype<? extends T>> PrototypeBuilder<T, P> prototypeBuilder(Class<? extends P> protoClass)
    {
        Objects.requireNonNull(protoClass, "protoClass must not be null");
        if (BUILDER_MAP.containsKey(protoClass))
            return (PrototypeBuilder<T, P>) BUILDER_MAP.get(protoClass);

        DefaultProtoBuilder defaultProtoBuilder = protoClass.getDeclaredAnnotation(DefaultProtoBuilder.class);
        if (defaultProtoBuilder != null) {

            Class<? extends PrototypeBuilder<T, P>> builderClass = (Class<
                    ? extends PrototypeBuilder<T, P>>) defaultProtoBuilder.value();

            try {
                PrototypeBuilder<T, P> builder = builderClass.getDeclaredConstructor().newInstance();

                /* Avoid further object creation, reuse builder */
                BUILDER_MAP.put(protoClass, builder);
                return builder;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                LOGGER.error("failed to instantiate default builder {} for prototype {}", builderClass, protoClass, e);
            }
        }

        /* No need to check for superclasses / -interfaces */
        if (protoClass == Prototype.class)
            return null;

        Class<? extends Prototype<?>>[] interfaces = Stream.of(protoClass.getInterfaces())
                .filter(Prototype.class::isAssignableFrom).toArray(Class[]::new);
        if (interfaces.length != 1) {
            LOGGER.atWarn().addKeyValue("interfaces", interfaces)
                    .log("cannot use superinterface builder detection for prototype {}", protoClass);
            return null;
        }
        return (PrototypeBuilder<T, P>) prototypeBuilder(interfaces[0]);
    }

    private static Matcher checkName(Pattern pattern, String name)
    {
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            LOGGER.error(Prototype.LOG_MARKER, "Prototype name {} does not match required pattern {}", name, pattern);
            throw new IllegalArgumentException(name);
        }
        return matcher;
    }

    public static void loadPrototypes(Path path, Executor executor) throws IOException
    {
        SerializationManager.loadGameData(
                Objects.requireNonNull(path, "path must not be null"), PrototypeManager::putPrototype, executor
        );
    }
}
