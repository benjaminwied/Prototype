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

import com.github.cleverelephant.prototype.PrototypeContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({ "javadoc", "static-method" })
class DefinitionDeserializerTest
{

    @Test
    void testAddDefinition() throws JsonMappingException, JsonProcessingException
    {
        String definition = """
                class "com.github.cleverelephant.prototypetest.TestPrototype"
                add "a": "a"
                add "b": 3.14
                add "array": [ "first", "second" ]
                add "object": {
                  "a": 1,
                  "b": false
                }
                """;

        String expectedResult = """
                {
                  "com.github.cleverelephant.prototypetest.TestPrototype" : {
                    "a": "a",
                    "b": 3.14,
                    "array": [ "first", "second" ],
                    "object": {
                      "a": 1,
                      "b": false
                    }
                  }
                }
                """;

        PrototypeDefinition def = DefinitionDeserializer.deserializeDefinition(definition);
        JsonNode node = def.getData(PrototypeContext.DEFAULT);
        JsonNode expectedNode = new ObjectMapper().readTree(expectedResult);

        assertEquals(expectedNode, node, "wrong data");
    }

}
