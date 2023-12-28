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
 * Maintains a list of all registered {@link Prototype Prototypes}.
 *
 * @author Benjamin Wied
 */
public final class PrototypeRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeRegistry.class);
    private static final Map<String, Prototype<?>> PROTOTYPES = new HashMap<>();

    private PrototypeRegistry()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return a (immutable) set containing the names of all registered {@link Prototype Prototypes}
     */
    public static synchronized Set<String> keys()
    {
        return Collections.unmodifiableSet(PROTOTYPES.keySet());
    }

    /**
     * Clears the registry, forcing all prototypes to regenerate.
     */
    public static synchronized void clear()
    {
        PROTOTYPES.clear();
    }

    /**
     * Registers all prototypes in the specified map.
     *
     * @param prototypes
     *                   to register
     *
     * @see              #register(Prototype)
     */
    public static synchronized void registerAll(Map<String, Prototype<?>> prototypes)
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
    public static synchronized void register(Prototype<?> prototype)
    {
        Objects.requireNonNull(prototype, "prototype must not be null");
        String name = prototype.name;
        if (PROTOTYPES.containsKey(name))
            LOGGER.warn("A prototype with name {} is already registered", name);

        PROTOTYPES.put(name, prototype);
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
        return Optional.empty();
    }

}
