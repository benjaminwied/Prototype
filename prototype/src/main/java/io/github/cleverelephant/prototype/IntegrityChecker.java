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

import java.lang.reflect.Field;
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
     * <li>Nested prototypes
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
            LOGGER.trace(Prototype.LOG_MARKER, "Verifying integrity of prototype {}", prototype.name);

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
            for (Field field : prototype.getClass().getFields()) {
                if (!field.getType().equals(PrototypeReference.class))
                    continue;

                PrototypeReference<?, ?> reference = (PrototypeReference<?, ?>) field.get(prototype);
                if (reference == null)
                    failure = verifyNullReference(field) || failure;
                else if (reference.getOptionalPrototype().isEmpty()) {
                    LOGGER.error(
                            Prototype.LOG_MARKER,
                            "Integrity of prototype {} invalid: no referenced prototype found: {}", prototype.name,
                            reference.getTargetPrototypeName()
                    );
                    failure = true;
                }
            }
        } catch (IllegalAccessException e) {
            LOGGER.error(Prototype.LOG_MARKER, "Failed to verify integrity of prototype {}", prototype.name, e);
            failure = true;
        }

        return failure;
    }

    private static boolean verifyNullReference(Field field)
    {
        return !field.isAnnotationPresent(OptionalReference.class);
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

        for (Field field : prototype.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !field.canAccess(prototype)
                    || !Modifier.isPublic(field.getModifiers()))
                continue;

            failure = verifyFieldIntegrity(prototype, field) || failure;
        }

        return failure;
    }

    private static boolean verifyFieldIntegrity(Prototype<?> prototype, Field field)
    {
        if (Prototype.class.isAssignableFrom(field.getType())) {
            LOGGER.error(
                    Prototype.LOG_MARKER,
                    "Integrity of prototype {} invalid: prototype nesting not allowed (property {})", prototype.name,
                    field.getName()
            );
            return true;
        }

        return false;
    }
}
