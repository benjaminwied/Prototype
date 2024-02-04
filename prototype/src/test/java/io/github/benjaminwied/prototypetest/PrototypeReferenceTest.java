/**
 * MIT License
 *
 * Copyright (c) 2024 Benjamin Wied
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
package io.github.benjaminwied.prototypetest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.benjaminwied.prototype.PrototypeReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({ "javadoc", "static-method" })
class PrototypeReferenceTest
{
    static ObjectMapper OBJECT_MAPPER;

    @BeforeAll
    static void setUpBeforeClass()
    {
        OBJECT_MAPPER = new ObjectMapper();
    }

    @AfterAll
    static void tearDownAfterClass()
    {
        OBJECT_MAPPER = null;
    }

    @Test
    void testSubprotPath()
    {
        PrototypeReference<?, ?> reference = new PrototypeReference<>("abc", "#path/to/my/proto");
        assertEquals("abc/path/to/my/proto", reference.getTargetPrototypeName(), "wrong relative path");

        reference = new PrototypeReference<>("folder/abc", "#path/to/my/proto");
        assertEquals("folder/abc/path/to/my/proto", reference.getTargetPrototypeName(), "wrong relative path");

        reference = new PrototypeReference<>("folder/abc", "#proto");
        assertEquals("folder/abc/proto", reference.getTargetPrototypeName(), "wrong relative path");
    }

    @Test
    void testAbsolutePath()
    {
        PrototypeReference<?, ?> reference = new PrototypeReference<>("abc", "path/to/my/proto");
        assertEquals("path/to/my/proto", reference.getTargetPrototypeName(), "wrong absolte path");
    }

    @Test
    void testRelativePath()
    {
        PrototypeReference<?, ?> reference = new PrototypeReference<>("abc", "/path/to/my/proto");
        assertEquals("path/to/my/proto", reference.getTargetPrototypeName(), "wrong relative path");

        reference = new PrototypeReference<>("folder/abc", "/path/to/my/proto");
        assertEquals("folder/path/to/my/proto", reference.getTargetPrototypeName(), "wrong relative path");

        reference = new PrototypeReference<>("folder/abc", "/proto");
        assertEquals("folder/proto", reference.getTargetPrototypeName(), "wrong relative path");
    }

    @Test
    void testPathCompression()
    {
        PrototypeReference<?, ?> reference = new PrototypeReference<>("abc", "path/./to/_my/././proto");
        assertEquals("path/to/_my/proto", reference.getTargetPrototypeName(), "wrong compressed path");

        reference = new PrototypeReference<>("abc", "path/to/my/other/../proto");
        assertEquals("path/to/my/proto", reference.getTargetPrototypeName(), "wrong compressed path");

        reference = new PrototypeReference<>("abc", "path/to/my/other/./../proto");
        assertEquals("path/to/my/proto", reference.getTargetPrototypeName(), "wrong compressed path");
    }

    @Test
    void testDeserialization() throws JsonMappingException, JsonProcessingException
    {
        String json = "\"/path/to/my/proto\"";
        InjectableValues injectableValues = new InjectableValues.Std().addValue("name", "folder/abc");
        PrototypeReference<?,
                ?> reference = OBJECT_MAPPER.reader(injectableValues).forType(PrototypeReference.class).readValue(json);
        assertEquals("folder/path/to/my/proto", reference.getTargetPrototypeName(), "deserialization failed");
    }

}
