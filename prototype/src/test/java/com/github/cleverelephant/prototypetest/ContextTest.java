package com.github.cleverelephant.prototypetest;

import com.github.cleverelephant.prototype.LuaInterpreter;
import com.github.cleverelephant.prototype.PrototypeContext;
import com.github.cleverelephant.prototype.PrototypeDefinition;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextTest
{

    @BeforeEach
    void setUp() throws Exception
    {
        Map<String, Object> context = new HashMap<>();
        context.put("foo", "bar");
        PrototypeContext.activateConent(context);
    }

    @AfterEach
    void tearDown() throws Exception
    {
        PrototypeContext.activateConent(Collections.emptyMap());
    }

    @Test
    void testContext() throws IOException, URISyntaxException
    {
        String json = Files.readString(Path.of(getClass().getResource("context.json").toURI()));
        String lua = Files.readString(Path.of(getClass().getResource("context.lua").toURI()));

        PrototypeDefinition def = new PrototypeDefinition();
        LuaInterpreter.INSTANCE.updatePrototype(def, lua);
        JsonNode node = LuaInterpreter.INSTANCE.evalPrototypeDefinition(def);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expectedNode = mapper.readTree(json);

        assertEquals(expectedNode, node, "wrong data");
    }

}
