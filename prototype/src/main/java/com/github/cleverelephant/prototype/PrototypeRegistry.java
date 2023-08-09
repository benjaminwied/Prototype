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
package com.github.cleverelephant.prototype;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains a list of all registered {@link PrototypeDefinition PrototypeDefinitions}.
 *
 * @author Benjamin Wied
 */
public final class PrototypeRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeRegistry.class);
    private static final Map<String, PrototypeDefinition> DEFINITIONS = new HashMap<>();
    private static final Map<String, Prototype<?>> PROTOTYPES = new HashMap<>();

    private PrototypeRegistry()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return a (immutable) set containing the names of all registered {@link PrototypeDefinition PrototypeDefinitions}
     */
    public static synchronized Set<String> keys()
    {
        return Collections.unmodifiableSet(DEFINITIONS.keySet());
    }

    /**
     * Unregisters all definitions.
     */
    public static synchronized void clear()
    {
        DEFINITIONS.clear();
        clearCache();
    }

    /**
     * Clears the cache, forcing all prototypes to regenerate.
     */
    public static synchronized void clearCache()
    {
        PROTOTYPES.clear();
        PrototypeContext.makeContextLive();
    }

    public static synchronized void register(String name, PrototypeDefinition definition)
    {
        Objects.requireNonNull(definition, "definition must not be null");
        if (DEFINITIONS.containsKey(name))
            LOGGER.warn("A definition with name {} is already registered", name);

        DEFINITIONS.put(name, definition);
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
    public static synchronized <P extends Prototype<?>> Optional<P> get(String name)
    {
        Objects.requireNonNull(name, "name must not be null");

        if (PROTOTYPES.containsKey(name))
            return Optional.of((P) PROTOTYPES.get(name));

        Optional<P> opt = Optional.ofNullable(DEFINITIONS.get(name))
                .map(LuaInterpreter.INSTANCE::evalPrototypeDefinition)
                .map(data -> SerializationManager.deserializePrototype(name, data));
        if (opt.isPresent())
            PROTOTYPES.put(name, opt.get());
        return opt;
    }

    /**
     * Returns a optional containing the registered definition, or an empty Optional is no definition is registered with
     * the given name.
     *
     * @param  name
     *              to query
     *
     * @return      the prototype
     */
    public static synchronized Optional<PrototypeDefinition> getDefinition(String name)
    {
        return Optional.ofNullable(DEFINITIONS.get(name));
    }

    /**
     * Returns a registered prototype, or create a new one if no prototype is registered with the given name.
     *
     * @param  name
     *              to query
     *
     * @return      the prototype
     */
    public static synchronized PrototypeDefinition getOrCreateDefinition(String name)
    {
        return DEFINITIONS.computeIfAbsent(name, __ -> new PrototypeDefinition());
    }

}
