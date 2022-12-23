/**
 * kindynos-core - Core engine of Kindynos
 * Copyright © 2022 Benjamin Wied (88872078+CleverElephant@users.noreply.github.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
