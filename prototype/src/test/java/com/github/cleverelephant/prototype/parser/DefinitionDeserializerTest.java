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
package com.github.cleverelephant.prototype.parser;

import com.github.cleverelephant.prototype.LuaInterpreter;
import com.github.cleverelephant.prototype.PrototypeDefinition;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({ "javadoc" })
class DefinitionDeserializerTest
{

    @Test
    void testAddDefinition() throws IOException, URISyntaxException
    {
        String json = Files.readString(Path.of(getClass().getResource("test.json").toURI()));
        String lua = Files.readString(Path.of(getClass().getResource("test.lua").toURI()));

        PrototypeDefinition def = new PrototypeDefinition();
        LuaInterpreter.INSTANCE.updatePrototype(def, lua);
        JsonNode node = LuaInterpreter.INSTANCE.evalPrototypeDefinition(def);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expectedNode = mapper.readTree(json);

        assertEquals(expectedNode, node, "wrong data");
    }

}
