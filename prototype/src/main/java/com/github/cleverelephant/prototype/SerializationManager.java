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
package com.github.cleverelephant.prototype;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads prototypes from json files using Jackson.<br>
 * <br>
 * <em>Methods of this class should usually not called manually, use {@link PrototypeManager} instead.</em>
 *
 * @author Benjamin Wied
 *
 * @see    Prototype
 * @see    PrototypeManager
 */
public final class SerializationManager
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path APPLICATION_ROOT = Path.of(".").toAbsolutePath().normalize();
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationManager.class);

    //    static {
    //        OBJECT_MAPPER.registerModule(new PrototypeMaterializationModule());
    //    }

    private SerializationManager()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Loads game data from a file or a directory. The {@link Consumer#accept(Object) consumer's accept function} is
     * called once for each prototype loaded.<br>
     * <br>
     * <em>This method should usually not called manually, use
     * {@link PrototypeManager#loadPrototypes(Path, ExecutorService)} instead.</em>
     *
     * @param  path
     *                              file or root directory
     * @param  consumer
     *                              to process prototypes
     * @param  loadingPool
     *                              to execute prototype loading in, or null to not use multithreading
     *
     * @throws IOException
     *                              if an error occurs
     * @throws NullPointerException
     *                              if either path or consumer is null
     *
     * @see                         PrototypeManager#loadPrototypes(Path, ExecutorService)
     */
    public static void loadGameData(Path path, Consumer<Prototype<?>> consumer, ExecutorService loadingPool)
            throws IOException
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

    private static void loadGameDataFromDirectory(
            Path path, Consumer<Prototype<?>> consumer, ExecutorService loadingPool
    ) throws IOException
    {
        Set<Future<?>> futures = new HashSet<>();

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (loadingPool == null)
                    loadGameDataFromFile(file, path.relativize(file), consumer);
                else {
                    Runnable load = () -> loadGameDataFromFile(file, path.relativize(file), consumer);
                    futures.add(loadingPool.submit(load));
                }
                return FileVisitResult.CONTINUE;
            }
        });

        for (Future<?> future : futures)
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                throw new PrototypeException(e.getCause() != null ? e.getCause() : e);
            }
    }

    private static boolean isFileValid(Path path)
    {
        return path.getFileName().toString().endsWith(".json");
    }

    private static void loadGameDataFromFile(Path path, Path relativePath, Consumer<Prototype<?>> consumer)
    {
        if (!isFileValid(path)) {
            LOGGER.atWarn().addMarker(Prototype.LOG_MARKER).log("skipping invalid file {}", compressLoggingPath(path));
            return;
        }

        try {
            LOGGER.atDebug().addMarker(Prototype.LOG_MARKER)
                    .log("loading game data from path {}", compressLoggingPath(path));
            Prototype<?> prototype = deserializePrototype(path, relativePath, new TypeReference<Prototype<?>>() {});
            consumer.accept(prototype);
        } catch (IOException e) {
            LOGGER.atError().addMarker(Prototype.LOG_MARKER).setCause(e)
                    .log("failed to load game data from path {}", path);
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

    /**
     * Deserializes a single prototype given its name and its class. The stream content must be valid json data. The
     * stream is closed after successful reading, so it should be closed externally.<br>
     * <br>
     * <em>This method should usually not called manually, use
     * {@link PrototypeManager#loadPrototypes(Path, ExecutorService)} instead.</em>
     *
     * @param  <T>
     *                              prototype generic type
     * @param  inputStream
     *                              data source
     * @param  name
     *                              prototype name
     * @param  clazz
     *                              prototype class
     *
     * @return                      the deserialized prototype
     *
     * @throws IOException
     *                              if an exception occurs
     * @throws NullPointerException
     *                              if any parameter is {@code null}
     *
     * @see                         #loadGameData(Path, Consumer, ExecutorService)
     * @see                         #prototypeNameFromPath(Path)
     */
    public static <
            T extends Prototype<?>> T deserializePrototype(InputStream inputStream, String name, TypeReference<T> clazz)
                    throws IOException
    {
        if (inputStream == null)
            throw new NullPointerException("inputStream must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");

        InjectableValues injectableValues = new InjectableValues.Std().addValue("name", name);
        return OBJECT_MAPPER.reader(injectableValues).forType(clazz).readValue(inputStream);
    }

    /**
     * Constructs the prototype name given a path relative to the prototype root.
     *
     * @param  relativePath
     *                              to construct name from
     *
     * @return                      the prototype name
     *
     * @throws NullPointerException
     *                              if {@code relativePath} is null
     */
    public static String prototypeNameFromPath(Path relativePath)
    {
        Objects.requireNonNull(relativePath, "relativePath must not be null");

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
