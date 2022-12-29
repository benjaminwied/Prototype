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
package com.github.cleverelephant.prototypetest;

import com.github.cleverelephant.prototype.PrototypeException;
import com.github.cleverelephant.prototype.SerializationManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("javadoc")
class SerializationTest
{
    @Test
    void test() throws IOException, URISyntaxException
    {
        Path dataPath = Path.of(getClass().getResource("test.json").toURI());

        TestPrototype proto;
        try (InputStream in = Files.newInputStream(dataPath)) {
            proto = SerializationManager.deserializePrototype(in, "test", new TypeReference<TestPrototype>() {});
        }

        assertAll(
                () -> assertEquals("test", proto.name(), "wrong name"),
                () -> assertEquals("a", proto.a(), "wrong data"), () -> assertEquals(1, proto.b(), "wrong data"),
                () -> assertTrue(proto.c(), "wrong data"), () -> assertEquals("abc", proto.d(), "wrong data"),
                () -> assertArrayEquals(new String[] { "first", "second" }, proto.array(), "wrong data"),
                () -> assertIterableEquals(
                        Arrays.asList(
                                new TestPrototype.SimpleContainer(1, "first"),
                                new TestPrototype.SimpleContainer(2, "second")
                        ), proto.generic(), "wrong generic data"
                ),
                () -> assertThrows(PrototypeException.class, () -> proto.missingDefault(), "missing data not missing")
        );
    }

}
