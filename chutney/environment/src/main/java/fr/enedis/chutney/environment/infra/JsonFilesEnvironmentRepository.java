/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.infra;

import static fr.enedis.chutney.tools.file.FileUtils.initFolder;
import static java.util.stream.Collectors.toList;

import fr.enedis.chutney.environment.domain.Environment;
import fr.enedis.chutney.environment.domain.EnvironmentRepository;
import fr.enedis.chutney.environment.domain.EnvironmentVariable;
import fr.enedis.chutney.environment.domain.Target;
import fr.enedis.chutney.environment.domain.exception.CannotDeleteEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import fr.enedis.chutney.environment.domain.exception.TargetAlreadyExistsException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import fr.enedis.chutney.tools.file.FileUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JsonFilesEnvironmentRepository implements EnvironmentRepository {

    private static final String JSON_FILE_EXT = ".json";

    private final Path storeFolderPath;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    public JsonFilesEnvironmentRepository(String storeFolderPath) throws UncheckedIOException {
        this.storeFolderPath = Paths.get(storeFolderPath).toAbsolutePath();
        initFolder(this.storeFolderPath);
    }

    @Override
    public synchronized void save(Environment environment) throws UnsupportedOperationException, InvalidEnvironmentNameException {
        doSave(environment);
    }

    @Override
    public Environment findByName(String name) throws EnvironmentNotFoundException {
        Path environmentPath = getEnvironmentPath(name);
        if (!Files.exists(environmentPath)) {
            throw new EnvironmentNotFoundException(environmentPath);
        }
        try {
            byte[] bytes = Files.readAllBytes(environmentPath);
            try {
                return objectMapper.readValue(bytes, JsonEnvironment.class).toEnvironment();
            } catch (IOException e) {
                throw new UnsupportedOperationException("Cannot deserialize configuration file: " + environmentPath, e);
            }
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot read configuration file: " + environmentPath, e);
        }
    }

    @Override
    public List<String> listNames() throws UnsupportedOperationException {
        return FileUtils.doOnListFiles(storeFolderPath, (pathStream) ->
            pathStream
                .filter(Files::isRegularFile)
                .filter(this::isJsonFile)
                .map(FileUtils::getNameWithoutExtension)
                .collect(toList())
        );
    }

    private boolean isJsonFile(Path path) {
        return path.getFileName().toString().endsWith(JSON_FILE_EXT);
    }

    @Override
    public void delete(String name) {
        Path environmentPath = getEnvironmentPath(name);
        if (!Files.exists(environmentPath)) {
            throw new EnvironmentNotFoundException(environmentPath);
        }
        try {
            Path backupPath = Paths.get(environmentPath.toString() + UUID.randomUUID().getMostSignificantBits() + ".backup");
            Files.move(environmentPath, backupPath);
        } catch (IOException e) {
            throw new CannotDeleteEnvironmentException(environmentPath, e);
        }
    }

    private void doSave(Environment environment) {
        Path environmentPath = getEnvironmentPath(environment.name);
        checkTargetNameUnicity(environment.targets);
        checkVariableNameUnicity(environment.variables);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(JsonEnvironment.from(environment));
            try {
                Files.write(environmentPath, bytes);
            } catch (IOException e) {
                throw new UnsupportedOperationException("Cannot write in configuration directory: " + storeFolderPath, e);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot serialize " + environment, e);
        }
    }

    private void checkTargetNameUnicity(Set<Target> targets) {
        Set<String> notUniqueTargets = targets
            .stream()
            .collect(Collectors.groupingBy(Target::getName, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (!notUniqueTargets.isEmpty()) {
            throw new TargetAlreadyExistsException("Targets are not unique : " + String.join(", ", notUniqueTargets));
        }
    }

    private void checkVariableNameUnicity(Set<EnvironmentVariable> variables) {
        Set<String> notUniqueVariables = variables
            .stream()
            .collect(Collectors.groupingBy(EnvironmentVariable::key, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (!notUniqueVariables.isEmpty()) {
            throw new VariableAlreadyExistingException("Variables are not unique : " + String.join(", ", notUniqueVariables));
        }
    }

    public Path getEnvironmentPath(String name) {
        return storeFolderPath.resolve(name + JSON_FILE_EXT);
    }
}
