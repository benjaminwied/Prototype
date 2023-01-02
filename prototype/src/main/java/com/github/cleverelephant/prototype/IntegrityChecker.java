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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies prototype integrity.
 *
 * @author Benjamin Wied
 *
 * @see    #verifyIntegrity()
 */
public final class IntegrityChecker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrityChecker.class);

    private IntegrityChecker()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Verifies the integrity of all registered Prototype definitions.<br>
     * <br>
     * This will check for
     * <ul>
     * <li>Missing properties with no default value,
     * <li>Default values wihout properties,
     * <li>Property & default value accessors that throw exceptions and
     * <li>Missing {@code PrototypeReference}.
     * </ul>
     * If the check passes, this method returns. Otherwise, an {@code PrototypeException} is thrown.
     *
     * @throws PrototypeException
     *                            if an integrity problem is encountered.
     */
    public static void verifyIntegrity()
    {
        LOGGER.info(Prototype.LOG_MARKER, "Verifying integrity...");

        boolean failure = false;

        for (Prototype<?> prototype : PrototypeManager.allPrototypes()) {
            LOGGER.trace(Prototype.LOG_MARKER, "Verifying integrity of prototype {}", prototype.name());

            failure = verifyIntegrity(prototype) || failure;
        }

        if (failure)
            throw new PrototypeException("Integrity problems found");

        LOGGER.info(Prototype.LOG_MARKER, "Integrity valid");
    }

    /**
     * Returns true if and only if an integrity problem is detected with the given {@link Prototype}.
     *
     * @param  prototype
     *                   to verify
     *
     * @return           true is an problem is encountered, false is everything is okay.
     */
    public static boolean verifyIntegrity(Prototype<?> prototype)
    {
        return verifySelfContaintedIntegrity(prototype) || verifyReferenceIntegrity(prototype);
    }

    /**
     * Verifies that all referenced prototypes exist.
     *
     * @param  prototype
     *                   to verify
     *
     * @return           true is an problem is encountered, false is everything is okay.
     */
    public static boolean verifyReferenceIntegrity(Prototype<?> prototype)
    {
        boolean failure = false;

        try {
            for (Method method : prototype.getClass().getMethods()) {
                if (!method.getReturnType().equals(PrototypeReference.class))
                    continue;

                PrototypeReference<?, ?> reference = (PrototypeReference<?, ?>) method.invoke(prototype);
                if (reference == null)
                    failure = verifyNullReference(prototype.name(), method) || failure;
                else if (reference.getOptionalPrototype().isEmpty()) {
                    LOGGER.error(
                            Prototype.LOG_MARKER,
                            "Integrity of prototype {} invalid: no referenced prototype found: {}", prototype.name(),
                            reference.getTargetPrototypeName()
                    );
                    failure = true;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error(Prototype.LOG_MARKER, "Failed to verify integrity of prototype {}", prototype.name(), e);
            failure = true;
        }

        return failure;
    }

    private static boolean verifyNullReference(String prototypeName, Method method)
    {
        if (method.isAnnotationPresent(OptionalReference.class) || method.getName().startsWith("$"))
            return false;

        try {
            method.getDeclaringClass().getMethod("$" + method.getName());
            return false;
        } catch (NoSuchMethodException | SecurityException e) {
            /* Can ignore e here */
            LOGGER.error(
                    Prototype.LOG_MARKER,
                    "Integrity of prototype {} invalid: PrototypeReference is null for property {}", prototypeName,
                    method.getName()
            );
            return true;
        }
    }

    /**
     * Verifies that the prototype definition is valid.
     *
     * @param  prototype
     *                   to verify
     *
     * @return           true is an problem is encountered, false is everything is okay.
     */
    public static boolean verifySelfContaintedIntegrity(Prototype<?> prototype)
    {
        Objects.requireNonNull(prototype, "cannot check integrity of null prototype");

        boolean failure = false;

        for (Method method : prototype.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || !method.canAccess(prototype)
                    || !Modifier.isPublic(method.getModifiers()) || method.getReturnType() == void.class
                    || method.getParameterCount() != 0)
                continue;

            failure = verifyMethodIntegrity(prototype, method) || failure;
        }

        return failure;
    }

    private static boolean verifyMethodIntegrity(Prototype<?> prototype, Method method)
    {
        boolean failure = false;

        if (method.getName().startsWith("$"))
            failure = verifyExistingValues(method, prototype.getClass(), prototype.name());

        if (Prototype.class.isAssignableFrom(method.getReturnType())) {
            LOGGER.error(
                    Prototype.LOG_MARKER,
                    "Integrity of prototype {} invalid: prototype nesting not allowed (property {})", prototype.name(),
                    method.getName()
            );
            failure = true;
        }

        if (!failure)
            try {
                /* This will check for missing values */
                method.invoke(prototype);
            } catch (IllegalAccessException | InvocationTargetException e) {
                Throwable x = e.getCause() != null ? e.getCause() : e;

                if (prototype.name().startsWith("$"))
                    LOGGER.error(
                            Prototype.LOG_MARKER,
                            "Integrity of prototype {} invalid: default value for property {} invalid",
                            prototype.name(), method.getName(), x
                    );
                else
                    LOGGER.error(
                            Prototype.LOG_MARKER, "Integrity of prototype {} invalid: no default value for property {}",
                            prototype.name(), method.getName(), x
                    );
                failure = true;
            }

        return failure;
    }

    private static boolean verifyExistingValues(Method method, Class<?> prototypeClass, String prototypeName)
    {
        boolean failure = false;

        try {
            Method m = prototypeClass.getMethod(method.getName().substring(1));
            if (!method.getReturnType().equals(m.getReturnType())) {
                failure = true;
                LOGGER.error(
                        Prototype.LOG_MARKER,
                        "Integrity of prototype {} invalid: default value type {} does not math required value type {}",
                        prototypeName, method.getReturnType(), m.getReturnType()
                );
            }
        } catch (NoSuchMethodException | SecurityException e) {
            failure = true;
            LOGGER.error(
                    Prototype.LOG_MARKER,
                    "Integrity of prototype {} invalid: value method for default value {} does not exist",
                    prototypeName, method.getName(), e
            );
        }

        return failure;
    }
}
