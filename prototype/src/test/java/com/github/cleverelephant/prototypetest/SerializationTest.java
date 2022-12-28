package com.github.cleverelephant.prototypetest;

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
                )
        );
    }

}
