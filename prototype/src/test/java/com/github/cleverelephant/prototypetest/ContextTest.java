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
package com.github.cleverelephant.prototypetest;

import com.github.cleverelephant.prototype.LuaInterpreter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContextTest
{
    Map<String, Object> context;

    @BeforeEach
    void setUp() throws Exception
    {
        context = new HashMap<>();
        context.put("foo", "bar");
    }

    @AfterEach
    void tearDown() throws Exception
    {
        context = null;
    }

    @Test
    void testContext() throws IOException, URISyntaxException
    {
        String json = Files.readString(Path.of(getClass().getResource("context.json").toURI()));

        LuaInterpreter interpreter = new LuaInterpreter(context);
        interpreter.runScript("com.github.cleverelephant.prototypetest.context.lua");
        ObjectNode prototypes = interpreter.computeData();
        assertEquals(1, prototypes.size(), "wrong number of prototypes");
        JsonNode node = prototypes.get("context");
        assertNotNull(node, "missing test prototype");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode expectedNode = mapper.readTree(json);

        assertEquals(expectedNode, node, "wrong data");
    }

}
