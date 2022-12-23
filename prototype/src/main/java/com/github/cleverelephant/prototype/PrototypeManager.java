/**
 * kindynos-core - Core engine of Kindynos
 * Copyright © 2022 Benjamin Wied (88872078+CleverElephant@users.noreply.github.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
