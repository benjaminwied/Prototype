package com.github.cleverelephant.prototypetest;

import com.github.cleverelephant.prototype.Prototype;
import com.github.cleverelephant.prototype.SerializationManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationTest
{
    List<Prototype<?>> prototypes;

    @BeforeEach
    void setUp() throws Exception
    {
        prototypes = new LinkedList<>();
    }

    @AfterEach
    void tearDown() throws Exception
    {
        prototypes = null;
    }

    @Test
    void test() throws IOException, URISyntaxException
    {
        Path dataPath = Path.of(getClass().getResource("test.json").toURI());
        SerializationManager.loadGameData(dataPath.getParent(), prototypes::add);

        assertEquals(1, prototypes.size(), "wrong number of prototypes");
        Prototype<?> first = prototypes.get(0);
        TestPrototype proto = assertInstanceOf(TestPrototype.class, first, "wrong instance");

        assertAll(
                () -> assertEquals("test", proto.name(), "wrong name"),
                () -> assertEquals("a", proto.a(), "wrong data"), () -> assertEquals(1, proto.b(), "wrong data"),
                () -> assertTrue(proto.c(), "wrong data")
        );
    }

}
