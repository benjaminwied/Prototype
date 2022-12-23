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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("javadoc")
@Slf4j
public final class PrototypeManager
{
    private static final Pattern PROTOTYPE_WITH_ARG_PATTERN = Pattern
            .compile("^(?<name>\\w+)(?:\\[(?<arg>\\w+)\\])?$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PROTOTYPE_NAME_PATTERN = Pattern
            .compile("^\\w+(?:/\\w+)*$", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Map<String, Prototype<?>> PROTOTYPE_MAP = new HashMap<>();
    private static final Map<Class<? extends Prototype<?>>, PrototypeBuilder<?, ?>> BUILDER_MAP = new HashMap<>();

    private PrototypeManager()
    {
        throw new UnsupportedOperationException();
    }

    public static <T, P extends Prototype<? extends T>> void registerBuilder(
            @NonNull Class<P> prototypeClass, @NonNull PrototypeBuilder<T, P> builder
    )
    {
        PrototypeBuilder<?, ?> old = BUILDER_MAP.put(prototypeClass, builder);

        if (old != null)
            log.warn(
                    Prototype.LOG_MARKER, "Added builder {} for prototype {}, replacing old builder {}",
                    builder.getClass(), prototypeClass, old.getClass()
            );
        else
            log.trace(Prototype.LOG_MARKER, "Added builder {} for prototype {}", builder.getClass(), prototypeClass);
    }

    public static void putPrototype(@NonNull Prototype<?> prototype)
    {
        checkName(PROTOTYPE_NAME_PATTERN, prototype.getName());

        Prototype<?> old = PROTOTYPE_MAP.put(prototype.getName(), prototype);

        if (old != null)
            log.warn(
                    Prototype.LOG_MARKER, "Added prototype '{}' to collection, replacing old value", prototype.getName()
            );
        else
            log.trace(Prototype.LOG_MARKER, "Added prototype '{}' to collection", prototype.getName());
    }

    public static <T,
            P extends Prototype<T>> Optional<P> getPrototype(@NonNull Class<T> typeClass, @NonNull String name)
    {
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
            log.trace(Prototype.LOG_MARKER, "No prototypes found for name {}", name, typeClass);
            return Optional.empty();
        }

        return Optional.of((P) prototype);
    }

    public static <T> T createType(@NonNull Class<T> typeClass, @NonNull String name)
    {
        return optionalCreateType(typeClass, name)
                .orElseThrow(() -> new IllegalArgumentException(name + "/" + typeClass));
    }

    public static <T> Optional<T> optionalCreateType(@NonNull Class<T> typeClass, @NonNull String name)
    {
        Matcher matcher = checkName(PROTOTYPE_WITH_ARG_PATTERN, name);

        Optional<Prototype<T>> prototype = getPrototype(typeClass, matcher.group("name"));
        if (prototype.isEmpty())
            return Optional.empty();
        return Optional.of(createType(prototype.get(), matcher.group("arg")));
    }

    public static <T, P extends Prototype<? extends T>> T createType(@NonNull P proto)
    {
        return createType(proto, null);
    }

    public static <T, P extends Prototype<? extends T>> T createType(@NonNull P proto, String arg)
    {
        PrototypeBuilder<T, P> builder = prototypeBuilder(proto.getClass());
        if (builder == null)
            throw new UnsupportedOperationException("no builder found for prototype " + proto);

        return builder.build(proto, arg);
    }

    @SuppressWarnings("unchecked")
    public static <T, P extends Prototype<? extends T>> PrototypeBuilder<T, P> prototypeBuilder(
            @NonNull Class<? extends P> protoClass
    )
    {
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
                log.error("failed to instantiate default builder {} for prototype {}", builderClass, protoClass, e);
            }
        }

        /* No need to check for superclasses / -interfaces */
        if (protoClass == Prototype.class)
            return null;

        Class<?> superClass = protoClass.getSuperclass();
        if (superClass != null && Prototype.class.isAssignableFrom(superClass))
            return prototypeBuilder(superClass.asSubclass(Prototype.class));

        Class<? extends Prototype<?>>[] interfaces = Stream.of(protoClass.getInterfaces())
                .filter(Prototype.class::isAssignableFrom).toArray(Class[]::new);
        if (interfaces.length != 1) {
            log.atWarn().addKeyValue("interfaces", interfaces)
                    .log("cannot use superinterface builder detection for prototype {}", protoClass);
            return null;
        }
        return (PrototypeBuilder<T, P>) prototypeBuilder(interfaces[0]);
    }

    private static Matcher checkName(Pattern pattern, String name)
    {
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            log.error(Prototype.LOG_MARKER, "Prototype name {} does not match required pattern {}", name, pattern);
            throw new IllegalArgumentException(name);
        }
        return matcher;
    }

    public static void loadPrototypes(@NonNull Path path) throws IOException
    {
        SerializationManager.loadGameData(path, PrototypeManager::putPrototype);
    }
}
