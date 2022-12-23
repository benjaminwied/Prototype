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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("javadoc")
@Slf4j
public final class SerializationManager
{
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final Marker LOG_MARKER = MarkerFactory.getMarker("io");
    private static final Path APPLICATION_ROOT = Path.of(".").toAbsolutePath().normalize();

    static {
        OBJECT_MAPPER.registerModule(new MrBeanModule());
    }

    private SerializationManager()
    {
        throw new UnsupportedOperationException();
    }

    public static void loadGameData(@NonNull Path path, @NonNull Consumer<Prototype<?>> consumer) throws IOException
    {
        if (Files.isDirectory(path))
            loadGameDataFromDirectory(path, consumer);
        else if (Files.isRegularFile(path))
            /* Empty relative path */
            loadGameDataFromFile(path, path.relativize(path), consumer);
        else
            throw new PrototypeException("failed to load game data from " + path);
    }

    private static void loadGameDataFromDirectory(@NonNull Path path, @NonNull Consumer<Prototype<?>> consumer)
            throws IOException
    {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                loadGameDataFromFile(file, path.relativize(file), consumer);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void loadGameDataFromFile(
            @NonNull Path path, @NonNull Path relativePath, @NonNull Consumer<Prototype<?>> consumer
    ) throws IOException
    {
        try {
            log.atDebug().addMarker(LOG_MARKER).log("loading game data from path {}", compressLoggingPath(path));
            Prototype<?> prototype = deserializePrototype(path, relativePath, new TypeReference<Prototype<?>>() {});
            consumer.accept(prototype);
        } catch (IOException e) {
            log.atError().addMarker(LOG_MARKER).setCause(e).log("failed to load game data from path {}", path);
            throw new IOException("failed to load game data", e);
        }
    }

    private static <T extends Prototype<?>> T deserializePrototype(
            @NonNull Path path, @NonNull Path relativePath, @NonNull TypeReference<T> clazz
    ) throws IOException
    {
        try (InputStream in = Files.newInputStream(path)) {
            String prototypeName = prototypeNameFromPath(relativePath);
            InjectableValues injectableValues = new InjectableValues.Std().addValue("name", prototypeName);
            return OBJECT_MAPPER.reader(injectableValues).forType(clazz).readValue(in);
        }
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
