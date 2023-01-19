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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SuppressWarnings({ "javadoc", "static-method" })
class DefinitionDeserializerTest
{

    @Test
    void testAddDefinition() throws JsonMappingException, JsonProcessingException
    {
        String data = """
                  "com.github.cleverelephant.prototypetest.TestPrototype" : {
                  "a" : "a",
                  "b" : 1,
                  "c" : true,
                  "array" : [ "first", "second" ],
                  "doubleValue" : 3.141592,
                  "generic" : [ {
                     "x" : 1,
                     "y" : "first"
                  }, {
                     "x" : 2,
                     "y" : "second"
                  } ]
                }
                              """;
        String definition = "add " + data;
        String result = "{" + data + "}";

        Action action = DefinitionDeserializer.deserializeDefinition(definition);

        AddAction addAction = assertInstanceOf(AddAction.class, action, "wrong action type");
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
        addAction.apply(node);

        JsonNode expectedNode = new ObjectMapper().readTree(result);

        assertEquals(expectedNode, node, "wrong data");
    }

}
