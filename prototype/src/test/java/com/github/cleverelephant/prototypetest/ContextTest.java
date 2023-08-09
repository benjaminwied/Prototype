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
