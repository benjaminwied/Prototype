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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final String REF_NULL = "reference" + NULL;

    private static final Pattern PROTOTYPE_NAME_PATTERN = Pattern
            .compile("^\\w+(?:/\\w+)*$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeManager.class);

    private final Map<String, Prototype<?>> prototypes;

    public PrototypeManager()
    {
        prototypes = new HashMap<>();
    }

    /**
     * @return a unmodifiable snapshot of all prototypes currently registered
     */
    public Set<Prototype<?>> allPrototypes()
    {
        return Collections.unmodifiableSet(
                new HashSet<>(keys().stream().map(this::get).filter(Optional::isPresent).map(Optional::get).toList())
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
    public <T, P extends Prototype<T>> Optional<P> getPrototype(String name)
    {
        Objects.requireNonNull(name, NAME_NULL);
        checkName(name);

        return getPrototypeNoNameCheck(name);
    }

    private <P extends Prototype<?>> Optional<P> getPrototypeNoNameCheck(String name)
    {
        Optional<P> prototype = this.<P>get(name);
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
    public <T> T createType(String name)
    {
        Objects.requireNonNull(name, NAME_NULL);

        return this.<T>optionalCreateType(name).orElseThrow(() -> new IllegalArgumentException(name));
    }

    /**
     * Returns an Optional containing the prototype the reference points to, or an empty Optional if no prototype could
     * be found.
     *
     * @param  <T>
     *                                  type
     * @param  <P>
     *                                  prototype
     * @param  reference
     *                                  prototype reference
     *
     * @return                          the prototype with the given name
     *
     * @throws NullPointerException
     *                                  if name is null
     * @throws IllegalArgumentException
     *                                  if name is invalid
     */
    public <T, P extends Prototype<T>> Optional<P> getPrototype(PrototypeReference<T, P> reference)
    {
        Objects.requireNonNull(reference, REF_NULL);
        return getPrototypeNoNameCheck(reference.getTargetPrototypeName());
    }

    /**
     * Builds a type for the given reference. If no such prototype could be found, fail with an
     * {@link IllegalArgumentException}.
     *
     * @param  <T>
     *                                  type
     * @param  reference
     *                                  prototype reference
     *
     * @return                          the type built
     *
     * @throws IllegalArgumentException
     *                                  if not prototype was found
     *
     * @see                             Prototype#build()
     * @see                             #optionalCreateType(String)
     */
    public <T> T createType(PrototypeReference<T, ?> reference)
    {
        Objects.requireNonNull(reference, REF_NULL);
        return createType(reference.getTargetPrototypeName());
    }

    /**
     * Builds a type using the prototype for the given reference. If no such prototype could be found, returns an empty
     * optional.
     *
     * @param  <T>
     *                                       type
     * @param  reference
     *                                       prototype reference
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
    public <T> Optional<T> optionalCreateType(PrototypeReference<T, ?> reference)
    {
        Objects.requireNonNull(reference, REF_NULL);
        return optionalCreateType(reference.getTargetPrototypeName());
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
    public <T> Optional<T> optionalCreateType(String name)
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
     * @return a (immutable) set containing the names of all registered {@link Prototype Prototypes}
     */
    public synchronized Set<String> keys()
    {
        return Collections.unmodifiableSet(prototypes.keySet());
    }

    /**
     * Clears the registry, forcing all prototypes to regenerate.
     */
    public synchronized void clear()
    {
        prototypes.clear();
    }

    /**
     * Registers all prototypes in the specified map.
     *
     * @param prototypes
     *                   to register
     *
     * @see              #register(Prototype)
     */
    public synchronized void registerAll(Map<String, Prototype<?>> prototypes)
    {
        for (Map.Entry<String, Prototype<?>> entry : prototypes.entrySet())
            register(entry.getValue());
    }

    /**
     * Registers the specified prototype, replacing any previously registered prototype with the same name.
     *
     * @param  prototype
     *                              to register, must not be null
     *
     * @throws NullPointerException
     *                              if the prototype is null
     */
    public synchronized void register(Prototype<?> prototype)
    {
        Objects.requireNonNull(prototype, "prototype must not be null");
        String name = prototype.name();
        if (prototypes.containsKey(name))
            LOGGER.warn("A prototype with name {} is already registered", name);

        prototypes.put(name, prototype);
    }

    /**
     * Returns a optional containing the registered prototype, or an empty Optional is no prototype is registered with
     * the given name.
     *
     * @param  <P>
     *              prototype class
     * @param  name
     *              to query
     *
     * @return      the prototype
     */
    @SuppressWarnings("unchecked")
    private synchronized <P extends Prototype<?>> Optional<P> get(String name)
    {
        Objects.requireNonNull(name, "name must not be null");

        if (prototypes.containsKey(name))
            return Optional.of((P) prototypes.get(name));
        return Optional.empty();
    }
}
