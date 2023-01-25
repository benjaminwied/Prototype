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

import com.github.cleverelephant.prototype.parser.PrototypeDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Maintains a list of all registered {@link PrototypeDefinition PrototypeDefinitions}.
 *
 * @author Benjamin Wied
 */
public final class PrototypeRegistry
{
    private static final Map<String, PrototypeDefinition> DEFINITIONS = new HashMap<>();
    private static final Map<String, Prototype<?>> PROTOTYPES = new HashMap<>();

    private static PrototypeContext currentContext = PrototypeContext.DEFAULT;

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
    }

    /**
     * Registers a new definition with the specified name.
     * <ul>
     * <li>If an definition with the given name already exists, both definitions are
     * {@link PrototypeDefinition#merge(PrototypeDefinition) merged}.
     * <li>Otherwise, the given definition is stored as-is.
     * </ul>
     *
     * @param name
     *                   prototype name
     * @param definition
     *                   prototype definition
     */
    public static synchronized void register(String name, PrototypeDefinition definition)
    {
        Objects.requireNonNull(definition, "definition must not be null");
        DEFINITIONS.merge(name, definition, (__, old) -> old.merge(definition));
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

        Optional<P> opt = Optional.ofNullable(DEFINITIONS.get(name)).map(def -> def.getData(currentContext))
                .map(data -> SerializationManager.deserializePrototype(data, name));
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
     * @return the current context
     *
     * @see    PrototypeContext
     */
    public static synchronized PrototypeContext getCurrentContext()
    {
        return currentContext;
    }

    /**
     * Activates a context to generate prototypes. This will also clear the cache.
     *
     * @param context
     *                to activate
     */
    public static synchronized void activateContext(PrototypeContext context)
    {
        if (context == currentContext)
            return;
        currentContext = Objects.requireNonNull(context, "context must not be null");
        clearCache();
    }

}
