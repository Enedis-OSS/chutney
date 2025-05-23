/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra.storage;

import static fr.enedis.chutney.tools.file.FileUtils.initFolder;
import static java.util.Optional.of;

import fr.enedis.chutney.server.core.domain.tools.ZipUtils;
import fr.enedis.chutney.tools.ThrowingRunnable;
import fr.enedis.chutney.tools.ThrowingSupplier;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.Files;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipOutputStream;

public class JsonFileAgentNetworkDao {

    static final Path ROOT_DIRECTORY_NAME = Paths.get("agents");
    static final String AGENTS_FILE_NAME = "endpoints.json";
    private final ObjectMapper objectMapper;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    private final File file;

    public JsonFileAgentNetworkDao(String storeFolderPath) {
        this(storeFolderPath, buildObjectMapper());
    }

    JsonFileAgentNetworkDao(String storeFolderPath, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Path dir = Paths.get(storeFolderPath).resolve(ROOT_DIRECTORY_NAME).toAbsolutePath();
        initFolder(dir);
        this.file = dir.resolve(AGENTS_FILE_NAME).toFile();
        file.delete(); // TODO keep/refresh network configuration on restart
    }

    public Optional<AgentNetworkForJsonFile> read() {
        return executeWithLocking(rwLock.readLock(), () -> {
            if (!file.exists()) return Optional.empty();
            return of(objectMapper.readValue(file, AgentNetworkForJsonFile.class));
        });
    }

    public void save(AgentNetworkForJsonFile agentEndpointsConfiguration) {
        executeWithLocking(rwLock.writeLock(), (ThrowingRunnable) () -> {
            Files.createParentDirs(file);
            objectMapper.writeValue(file, agentEndpointsConfiguration);
        });
    }

    public void backup(OutputStream outputStream) {
        try (ZipOutputStream zipOutPut = new ZipOutputStream(new BufferedOutputStream(outputStream, 4096))) {
            ZipUtils.compressFile(this.file, this.file.getName(), zipOutPut);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T executeWithLocking(Lock lock, ThrowingSupplier<T, ? extends Exception> supplier) {
        lock.lock();
        try {
            return supplier.unsafeGet();
        } finally {
            lock.unlock();
        }
    }

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

        return objectMapper.setVisibility(
            objectMapper.getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
        );
    }
}
