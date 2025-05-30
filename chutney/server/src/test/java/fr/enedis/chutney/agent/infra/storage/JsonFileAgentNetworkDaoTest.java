/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra.storage;

import static fr.enedis.chutney.agent.infra.storage.JsonFileAgentNetworkDao.AGENTS_FILE_NAME;
import static fr.enedis.chutney.agent.infra.storage.JsonFileAgentNetworkDao.ROOT_DIRECTORY_NAME;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static util.WaitUtils.awaitDuring;

import fr.enedis.chutney.tools.ThrowingRunnable;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JsonFileAgentNetworkDaoTest {

    private static final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();

    private File file;
    private JsonFileAgentNetworkDao sut;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        sut = new JsonFileAgentNetworkDao(tempDir.toAbsolutePath().toString(), objectMapper);
        Path filePath = tempDir.resolve(ROOT_DIRECTORY_NAME).resolve(AGENTS_FILE_NAME);
        file = Files.createFile(filePath).toFile();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void read_should_return_the_network() throws Exception {
        file.createNewFile();
        AgentNetworkForJsonFile agentNetwork = mock(AgentNetworkForJsonFile.class);
        when(objectMapper.<AgentNetworkForJsonFile>readValue(any(File.class), any(Class.class))).thenReturn(agentNetwork);
        Optional<AgentNetworkForJsonFile> result = sut.read();
        assertThat(result).hasValue(agentNetwork);
    }

    @Test
    public void read_without_existing_file_should_return_empty() throws IOException {
        Files.deleteIfExists(file.toPath());
        assertThat(sut.read()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void parallel_reading_is_possible() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);

        AgentNetworkForJsonFile agentNetwork = mock(AgentNetworkForJsonFile.class);
        when(objectMapper.<AgentNetworkForJsonFile>readValue(any(File.class), any(Class.class))).thenAnswer(stuff -> {
            tryAndKeepError(() -> barrier.await(1, TimeUnit.SECONDS));
            return agentNetwork;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> sut.read());
        executor.submit(() -> sut.read());
        executor.shutdown();
        assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).isEmpty();
    }

    @Test
    public void save_should_update_file() throws Exception {
        AgentNetworkForJsonFile agentNetwork = mock(AgentNetworkForJsonFile.class);
        sut.save(agentNetwork);
        verify(objectMapper).writeValue(file, agentNetwork);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void read_should_wait_a_write_to_end() throws Exception {
        AtomicBoolean reading = new AtomicBoolean(false);
        Semaphore enterWriting = new Semaphore(0);

        Semaphore readSubmitted = new Semaphore(0);

        file.createNewFile();
        AgentNetworkForJsonFile agentNetwork = mock(AgentNetworkForJsonFile.class);
        when(file.exists()).thenReturn(true);

        doAnswer(stuff -> {
            enterWriting.release();
            readSubmitted.tryAcquire(2, TimeUnit.SECONDS);
            if (reading.get()) errors.add("file has been read while writing");
            return null;
        }).when(objectMapper).writeValue(any(File.class), any(AgentNetworkForJsonFile.class));

        when(objectMapper.<AgentNetworkForJsonFile>readValue(any(File.class), any(Class.class))).thenAnswer(stuff -> {
            reading.set(true);
            return agentNetwork;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> sut.save(agentNetwork));
        assertThat(enterWriting.tryAcquire(2, TimeUnit.SECONDS)).isTrue();
        executor.submit(() -> sut.read());
        readSubmitted.release();
        executor.shutdown();
        assertThat(executor.awaitTermination(4, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).isEmpty();
    }

    @Test
    public void write_should_wait_a_write_to_end() throws Exception {
        AtomicBoolean otherWriting = new AtomicBoolean(false);
        Semaphore enterFirstWrite = new Semaphore(0);

        Semaphore secondWriteSubmitted = new Semaphore(0);

        AgentNetworkForJsonFile agentNetwork = mock(AgentNetworkForJsonFile.class);
        doAnswer(stuff -> {
            enterFirstWrite.release();
            secondWriteSubmitted.tryAcquire(2, TimeUnit.SECONDS);
            if (otherWriting.get()) errors.add("file has been written while writing");
            return null;
        }).doAnswer(stuff -> {
            otherWriting.set(true);
            return null;
        }).when(objectMapper).writeValue(any(File.class), any(AgentNetworkForJsonFile.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> sut.save(agentNetwork));
        assertThat(enterFirstWrite.tryAcquire(2, TimeUnit.SECONDS)).isTrue();
        executor.submit(() -> sut.save(agentNetwork));
        secondWriteSubmitted.release();
        executor.shutdown();
        assertThat(executor.awaitTermination(4, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).isEmpty();
    }

    @Test
    @Disabled("instable : depends on machine performance...")
    @SuppressWarnings("unchecked")
    public void write_should_wait_a_read_to_end() throws Exception {
        AtomicBoolean writing = new AtomicBoolean(false);
        Semaphore reading = new Semaphore(0);

        AgentNetworkForJsonFile agentNetwork = mock(AgentNetworkForJsonFile.class);
        when(objectMapper.<AgentNetworkForJsonFile>readValue(same(file), any(Class.class))).thenAnswer(stuff -> {
            reading.release();
            awaitDuring(20, MILLISECONDS);
            if (writing.get()) errors.add("file has been written while reading");
            return null;
        });

        doAnswer(stuff -> {
            writing.set(true);
            return agentNetwork;
        }).when(objectMapper).writeValue(same(file), same(agentNetwork));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> sut.read());
        assertThat(reading.tryAcquire(40, TimeUnit.MILLISECONDS)).isTrue();
        executor.submit(() -> sut.save(agentNetwork));
        executor.shutdown();
        assertThat(executor.awaitTermination(200, TimeUnit.MILLISECONDS)).isTrue();

        assertThat(errors).isEmpty();
    }

    @Test
    public void should_backup_json_file_as_zip_file() throws IOException {
        // Given
        Path backup = Paths.get("./target/backup", "endpoints");
        Files.createDirectories(backup.getParent());

        Files.deleteIfExists(backup);
        file.createNewFile();

        try (OutputStream outputStream = Files.newOutputStream(Files.createFile(backup))) {
            // When
            sut.backup(outputStream);
        }

        // Then
        ZipFile zipFile = new ZipFile(backup.toString());
        List<String> entriesNames = zipFile.stream().map(ZipEntry::getName).collect(Collectors.toList());
        assertThat(entriesNames).containsExactly(AGENTS_FILE_NAME);
    }

    private void tryAndKeepError(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (TimeoutException e) {
            errors.add("timeout");
        } catch (Exception e) {
            errors.add(e.getMessage() != null ? e.getMessage() : "an error with no message occurred");
        }
    }
}
