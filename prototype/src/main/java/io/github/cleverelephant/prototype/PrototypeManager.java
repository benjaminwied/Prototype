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
package io.github.cleverelephant.prototype;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores prototypes for later retrieval and type generation.
 *
 * @author Benjamin Wied
 *
 * @see    Prototype
 */
public final class PrototypeManager
{
    private static final String NULL = " must not be null";
    private static final String NAME_NULL = "name" + NULL;

    private static final Pattern PROTOTYPE_NAME_PATTERN = Pattern
            .compile("^\\w+(?:/\\w+)*$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeManager.class);

    private PrototypeManager()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return a unmodifiable snapshot of all prototypes currently registered
     */
    public static Set<Prototype<?>> allPrototypes()
    {
        return Collections.unmodifiableSet(
                new HashSet<>(
                        PrototypeRegistry.keys().stream().map(PrototypeRegistry::get).filter(Optional::isPresent)
                                .map(Optional::get).toList()
                )
        );
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
        checkName(name);

        return getPrototypeNoNameCheck(name);
    }

    private static <P extends Prototype<?>> Optional<P> getPrototypeNoNameCheck(String name)
    {
        Optional<P> prototype = PrototypeRegistry.<P>get(name);
        if (prototype.isEmpty()) {
            LOGGER.trace(Prototype.LOG_MARKER, "No prototypes found for name {}", name);
            return Optional.empty();
        }

        return prototype;
    }

    /**
     * Builds a type using the prototype registered under the given name. If no such prototype could be found, fail with
     * an {@link IllegalArgumentException}.
     *
     * @param  <T>
     *                                  type
     * @param  name
     *                                  prototype name
     *
     * @return                          the type built
     *
     * @throws IllegalArgumentException
     *                                  if not prototype was found
     *
     * @see                             Prototype#build()
     * @see                             #optionalCreateType(String)
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
     * @see                                  Prototype#build()
     * @see                                  #createType(String)
     */
    public static <T> Optional<T> optionalCreateType(String name)
    {
        Objects.requireNonNull(name, NAME_NULL);
        checkName(name);

        Optional<Prototype<T>> prototype = getPrototype(name);
        return prototype.map(Prototype::build);
    }

    /**
     * Checks if the given string matches conditions for a prototype name. This method either passes if the name is
     * valid, or throws an {@link IllegalArgumentException} if not.
     *
     * @param  name
     *                                  to check
     *
     * @throws IllegalArgumentException
     *                                  if the name is invalid
     */
    public static void checkName(String name)
    {
        Matcher matcher = PROTOTYPE_NAME_PATTERN.matcher(name);
        if (!matcher.matches()) {
            LOGGER.error(Prototype.LOG_MARKER, "Prototype name {} does not match required pattern", name);
            throw new IllegalArgumentException(name);
        }
    }

    /**
     * Loads all prototypes found in the given file or directory, and all child directories and adds them to the
     * collection.
     *
     * @param  path
     *                     to search for prototypes
     * @param  context
     *                     prototype context to use
     *
     * @throws IOException
     *                     if an IOException occurs while reading prototypes
     *
     * @see                SerializationManager#loadGameData(Path, java.util.function.Consumer, ExecutorService)
     */
    public static void loadPrototypes(Path path, Map<String, Object> context) throws IOException
    {
        LuaInterpreter interpreter = new LuaInterpreter(path, context);
        SerializationManager.loadGameData(Objects.requireNonNull(path, "path" + NULL), interpreter::runScript, null);
        PrototypeRegistry.registerAll(SerializationManager.deserializePrototypes(interpreter.computeData(), context));
    }
}
