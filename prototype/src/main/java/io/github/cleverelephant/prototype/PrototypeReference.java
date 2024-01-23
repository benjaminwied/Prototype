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

import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;

/**
 * A reference to a prototype, usually used from within a prototype definition / builder.
 *
 * @author     Benjamin Wied
 *
 * @param  <T>
 *             type
 * @param  <P>
 *             prototype
 */
public final class PrototypeReference<T, P extends Prototype<T>>
{
    private static final Pattern RELATIVE_PARENT_REMOVE = Pattern
            .compile("/\\w+/\\.\\.", Pattern.UNICODE_CHARACTER_CLASS);

    private final String targetPrototypeName;

    /**
     * Constructs a new {@code PrototypeReference} using the given {@code sourcePrototypeName} and
     * {@code relativeTargetName}.<br>
     * <br>
     * Similar to {@link Class#getResource(String)}, if {@code relativeTargetName} starts with '/', it is considered the
     * absolute prototype name. If not, the absolute prototype name is constructed using the folder of
     * {@code sourcePrototypeName}.
     * <ul>
     * <li>If {@code relativeTargetName} starts with '/', it is considered to be relative to
     * {@code sourcePrototypeName}'s parent.
     * <li>If {@code relativeTargetName} starts with '#', it is considered to be relative to
     * {@code sourcePrototypeName}.
     * <li>If not, it is considered as the absolute prototype name.
     * </ul>
     *
     * @param  sourcePrototypeName
     *                                  name of the prototype this reference originates from
     * @param  relativeTargetName
     *                                  relative or absolute path to target prototype
     *
     * @throws NullPointerException
     *                                  if either {@code sourcePrototypeName} or {@code relativeTargetName} is null
     * @throws IllegalArgumentException
     *                                  if either {@code sourcePrototypeName} or the absolute path to the target
     *                                  prototype is not a valid prototype name (as defined by
     *                                  {@link PrototypeManager#checkName(String)}.
     */
    @JsonCreator
    public PrototypeReference(
            @JacksonInject(useInput = OptBoolean.FALSE, value = "name") String sourcePrototypeName,
            String relativeTargetName
    )
    {
        Objects.requireNonNull(sourcePrototypeName, "sourcePrototypeName must not be null");
        Objects.requireNonNull(relativeTargetName, "relativeTargetName must not be null");
        PrototypeManager.checkName(sourcePrototypeName);

        if (relativeTargetName.startsWith("#"))
            targetPrototypeName = compressPath(sourcePrototypeName + "/" + relativeTargetName.substring(1));
        else if (!relativeTargetName.startsWith("/"))
            targetPrototypeName = compressPath(relativeTargetName);
        else {
            String prototypePackage;
            if (sourcePrototypeName.contains("/"))
                prototypePackage = sourcePrototypeName.substring(0, sourcePrototypeName.lastIndexOf("/")) + "/";
            else
                prototypePackage = "";

            targetPrototypeName = compressPath(prototypePackage + relativeTargetName.substring(1));
        }

        PrototypeManager.checkName(targetPrototypeName);
    }

    /**
     * Constructs a new {@code PrototypeReference} using the given {@code name}.
     *
     * @param  name
     *                                  name of the prototype this reference points to
     *
     * @throws NullPointerException
     *                                  if {@code name} is null
     * @throws IllegalArgumentException
     *                                  if {@code name} is not a valid prototype name
     */
    public PrototypeReference(String name)
    {
        Objects.requireNonNull(name, "relativeTargetName must not be null");
        PrototypeManager.checkName(name);

        targetPrototypeName = name;
    }

    private static String compressPath(String path)
    {
        while (path.contains("/./"))
            path = path.replace("/./", "/");

        return RELATIVE_PARENT_REMOVE.matcher(path).replaceAll("");
    }

    /**
     * @return the name of the prototype this reference points to
     */
    public String getTargetPrototypeName()
    {
        return targetPrototypeName;
    }
}
