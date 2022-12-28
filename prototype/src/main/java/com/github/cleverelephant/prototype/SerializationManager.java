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
package com.github.cleverelephant.prototype;

import com.github.cleverelephant.prototype.materialization.PrototypeMaterializationModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@SuppressWarnings("javadoc")
public final class SerializationManager
{
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final Marker LOG_MARKER = MarkerFactory.getMarker("io");
    private static final Path APPLICATION_ROOT = Path.of(".").toAbsolutePath().normalize();
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationManager.class);

    static {
        OBJECT_MAPPER.registerModule(new PrototypeMaterializationModule());
    }

    private SerializationManager()
    {
        throw new UnsupportedOperationException();
    }

    public static void loadGameData(Path path, Consumer<Prototype<?>> consumer, Executor loadingPool) throws IOException
    {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        if (Files.isDirectory(path))
            loadGameDataFromDirectory(path, consumer, loadingPool);
        else if (Files.isRegularFile(path))
            /* Empty relative path */
            loadGameDataFromFile(path, path.relativize(path), consumer);
        else
            throw new PrototypeException("failed to load game data from " + path);
    }

    private static void loadGameDataFromDirectory(Path path, Consumer<Prototype<?>> consumer, Executor loadingPool)
            throws IOException
    {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (loadingPool != null)
                    loadingPool.execute(() -> loadGameDataFromFile(file, path.relativize(file), consumer));
                else
                    loadGameDataFromFile(file, path.relativize(file), consumer);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isFileValid(Path path)
    {
        return path.getFileName().toString().endsWith(".json");
    }

    private static void loadGameDataFromFile(Path path, Path relativePath, Consumer<Prototype<?>> consumer)
    {
        if (!isFileValid(path)) {
            LOGGER.atWarn().addMarker(LOG_MARKER).log("skipping invalid file {}", compressLoggingPath(path));
            return;
        }

        try {
            LOGGER.atDebug().addMarker(LOG_MARKER).log("loading game data from path {}", compressLoggingPath(path));
            Prototype<?> prototype = deserializePrototype(path, relativePath, new TypeReference<Prototype<?>>() {});
            consumer.accept(prototype);
        } catch (IOException e) {
            LOGGER.atError().addMarker(LOG_MARKER).setCause(e).log("failed to load game data from path {}", path);
            throw new PrototypeException("failed to load game data", e);
        }
    }

    private static <T extends Prototype<?>> T deserializePrototype(Path path, Path relativePath, TypeReference<T> clazz)
            throws IOException
    {
        try (InputStream in = Files.newInputStream(path)) {
            String prototypeName = prototypeNameFromPath(relativePath);
            return deserializePrototype(in, prototypeName, clazz);
        }
    }

    public static <
            T extends Prototype<?>> T deserializePrototype(InputStream inputStream, String name, TypeReference<T> clazz)
                    throws IOException
    {
        InjectableValues injectableValues = new InjectableValues.Std().addValue("name", name);
        return OBJECT_MAPPER.reader(injectableValues).forType(clazz).readValue(inputStream);
    }

    public static String prototypeNameFromPath(Path relativePath)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < relativePath.getNameCount(); i++) {
            if (i > 0)
                builder.append('/');

            if (i < relativePath.getNameCount() - 1)
                builder.append(relativePath.getName(i));
            else {
                String fileName = relativePath.getFileName().toString();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                builder.append(fileName);
            }
        }
        return builder.toString();
    }

    private static String compressLoggingPath(Path path)
    {
        if (path.startsWith(APPLICATION_ROOT))
            return "~\\" + APPLICATION_ROOT.relativize(path).toString();
        return prototypeNameFromPath(path);
    }
}
