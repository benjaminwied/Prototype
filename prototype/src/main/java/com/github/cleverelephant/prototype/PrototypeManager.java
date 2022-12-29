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

/**
 * Stores prototypes for later retrieval and type generation.
 *
 * @author Benjamin Wied
 *
 * @see    Prototype
 * @see    PrototypeBuilder
 */
public final class PrototypeManager
{
    private static final String NULL = " must not be null";
    private static final String NAME_NULL = "name" + NULL;

    private static final Pattern PROTOTYPE_NAME_PATTERN = Pattern
            .compile("^\\w+(?:/\\w+)*$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeManager.class);

    private static final Map<String, Prototype<?>> PROTOTYPE_MAP = new HashMap<>();
    private static final Map<Class<? extends Prototype<?>>, PrototypeBuilder<?, ?>> BUILDER_MAP = new HashMap<>();

    private PrototypeManager()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Registers a builder to build to prototype of the specified class. This will override any builder registered
     * earlier and implicit builder resolving.
     *
     * @param  <T>
     *                              type
     * @param  <P>
     *                              prototype
     * @param  prototypeClass
     *                              prototype class
     * @param  builder
     *                              builder to use
     *
     * @see                         #prototypeBuilder(Class)
     *
     * @throws NullPointerException
     *                              if prototypeClass of builder is null
     */
    public static <T, P extends Prototype<? extends T>> void registerBuilder(
            Class<P> prototypeClass, PrototypeBuilder<T, P> builder
    )
    {
        Objects.requireNonNull(prototypeClass, "prototypeClass" + NULL);
        Objects.requireNonNull(builder, "builder" + NULL);

        PrototypeBuilder<?, ?> old = BUILDER_MAP.put(prototypeClass, builder);

        if (old != null)
            LOGGER.warn(
                    Prototype.LOG_MARKER, "Added builder {} for prototype {}, replacing old builder {}",
                    builder.getClass(), prototypeClass, old.getClass()
            );
        else
            LOGGER.trace(Prototype.LOG_MARKER, "Added builder {} for prototype {}", builder.getClass(), prototypeClass);
    }

    /**
     * Adds a prototype to the collection.
     *
     * @param  prototype
     *                                  to add
     *
     * @throws NullPointerException
     *                                  if prototype is null
     * @throws IllegalArgumentException
     *                                  if prototype.name() is an invalid name
     */
    public static void putPrototype(Prototype<?> prototype)
    {
        Objects.requireNonNull(prototype, "prototype" + NULL);
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

    /**
     * Returns an Optional containing the prototype with the given name, or an empty Optional if no prototype could be
     * found.
     *
     * @param  <T>
     *                                  type
     * @param  <P>
     *                                  prototype
     * @param  name
     *                                  prototype name
     *
     * @return                          the prototype with the given name
     *
     * @throws NullPointerException
     *                                  if name is null
     * @throws IllegalArgumentException
     *                                  if name is invalid
     */
    public static <T, P extends Prototype<T>> Optional<P> getPrototype(String name)
    {
        Objects.requireNonNull(name, NAME_NULL);
        checkName(PROTOTYPE_NAME_PATTERN, name);

        return getPrototypeNoNameCheck(name);
    }

    @SuppressWarnings("unchecked")
    private static <P extends Prototype<?>> Optional<P> getPrototypeNoNameCheck(String name)
    {
        Prototype<?> prototype = PROTOTYPE_MAP.get(name);
        if (prototype == null) {
            LOGGER.trace(Prototype.LOG_MARKER, "No prototypes found for name {}", name);
            return Optional.empty();
        }

        return Optional.of((P) prototype);
    }

    /**
     * Builds a type using the prototype registered under the given name. If no such prototype could be found, fail with
     * an {@link IllegalArgumentException}.
     *
     * @param  <T>
     *              type
     * @param  name
     *              prototype name
     *
     * @return      the type built
     *
     * @see         #createType(Prototype)
     * @see         #optionalCreateType(String)
     */
    public static <T> T createType(String name)
    {
        Objects.requireNonNull(name, NAME_NULL);

        return PrototypeManager.<T>optionalCreateType(name).orElseThrow(() -> new IllegalArgumentException(name));
    }

    /**
     * Builds a type using the prototype registered under the given name. If no such prototype could be found, returns
     * an empty optional.
     *
     * @param  <T>
     *                                       type
     * @param  name
     *                                       prototype name
     *
     * @return                               an optional containing the type built, or an empty optional if no prototype
     *                                       was found
     *
     * @throws NullPointerException
     *                                       if name is null
     * @throws IllegalArgumentException
     *                                       if name is invalid
     * @throws UnsupportedOperationException
     *                                       if not builder was found for the given prototype
     *
     * @see                                  #createType(Prototype)
     * @see                                  #createType(String)
     */
    public static <T> Optional<T> optionalCreateType(String name)
    {
        Objects.requireNonNull(name, NAME_NULL);
        checkName(PROTOTYPE_NAME_PATTERN, name);

        Optional<Prototype<T>> prototype = getPrototype(name);
        return prototype.map(PrototypeManager::createType);
    }

    /**
     * Builds a type using the given prototype.
     *
     * @param  <T>
     *                                       type
     * @param  <P>
     *                                       prototype
     * @param  proto
     *                                       to build
     *
     * @return                               the built type
     *
     * @throws NullPointerException
     *                                       if proto is null
     * @throws UnsupportedOperationException
     *                                       if not builder was found for the given prototype
     *
     * @see                                  PrototypeBuilder#build(Prototype)
     */
    public static <T, P extends Prototype<? extends T>> T createType(P proto)
    {
        Objects.requireNonNull(proto, "proto" + NULL);

        PrototypeBuilder<T, P> builder = prototypeBuilder(proto.getClass());
        if (builder == null)
            throw new UnsupportedOperationException("no builder found for prototype " + proto);

        return builder.build(proto);
    }

    /**
     * Resolves the builder to use for prototypes of the given class.<br>
     * <br>
     * Builders are looked up in the following order:
     * <ol>
     * <li>Any builder explicitly registered with {@link #registerBuilder(Class, PrototypeBuilder)} for
     * {@code prototypeClass} (if any).
     * <li>The builder defined in the {@link DefaultProtoBuilder} annotation on {@code protoClass} (if any).
     * <li>Does the same for the super-prototype (the superinterface that is a prototype) if exactly one exists.
     * <li>returns {@code null}
     * </ol>
     *
     * @param  <T>
     *                              type
     * @param  <P>
     *                              prototype
     * @param  protoClass
     *                              prototypeClass to resolve builder for
     *
     * @return                      the builder to use for the given prototype class or {@code null} if no builder could
     *                              be resolved
     *
     * @throws NullPointerException
     *                              if protoClass is null
     *
     * @see                         #registerBuilder(Class, PrototypeBuilder)
     * @see                         #createType(Prototype)
     */
    @SuppressWarnings("unchecked")
    public static <T,
            P extends Prototype<? extends T>> PrototypeBuilder<T, P> prototypeBuilder(Class<? extends P> protoClass)
    {
        Objects.requireNonNull(protoClass, "protoClass" + NULL);
        if (BUILDER_MAP.containsKey(protoClass))
            return (PrototypeBuilder<T, P>) BUILDER_MAP.get(protoClass);

        PrototypeBuilder<T, P> builder = getDefaultProtoBuilder(protoClass);
        if (builder != null)
            return builder;

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

    @SuppressWarnings("unchecked")
    private static <T, P extends Prototype<? extends T>> PrototypeBuilder<T, P> getDefaultProtoBuilder(
            Class<? extends P> protoClass
    )
    {
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

        return null;
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

    /**
     * Loads all prototypes found in the given file or directory, and all child directories and adds them to the
     * collection.
     *
     * @param  path
     *                     to search for prototypes
     * @param  executor
     *                     if non-null, will be used to load prototypes
     *
     * @throws IOException
     *                     if an IOException occurs while reading prototypes
     *
     * @see                SerializationManager#loadGameData(Path, java.util.function.Consumer, Executor)
     */
    public static void loadPrototypes(Path path, Executor executor) throws IOException
    {
        SerializationManager
                .loadGameData(Objects.requireNonNull(path, "path" + NULL), PrototypeManager::putPrototype, executor);
    }
}
