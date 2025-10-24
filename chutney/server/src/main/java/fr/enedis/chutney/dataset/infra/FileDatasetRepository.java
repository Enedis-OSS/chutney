/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.infra;

import static fr.enedis.chutney.config.ServerConfigurationValues.CONFIGURATION_FOLDER_SPRING_VALUE;
import static fr.enedis.chutney.dataset.infra.DatasetMapper.fromDto;
import static fr.enedis.chutney.dataset.infra.DatasetMapper.toDto;
import static fr.enedis.chutney.tools.file.FileUtils.createFile;
import static fr.enedis.chutney.tools.file.FileUtils.initFolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.enedis.chutney.dataset.domain.DataSetRepository;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.dataset.DataSetAlreadyExistException;
import fr.enedis.chutney.server.core.domain.dataset.DataSetNotFoundException;
import fr.enedis.chutney.tools.file.FileUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileDatasetRepository implements DataSetRepository {

    private static final String FILE_EXTENSION = ".json";

    static final Path ROOT_DIRECTORY_NAME = Paths.get("dataset");

    private final Path storeFolderPath;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .enable(SerializationFeature.INDENT_OUTPUT);

    FileDatasetRepository(@Value(CONFIGURATION_FOLDER_SPRING_VALUE) String storeFolderPath) throws UncheckedIOException {
        this.storeFolderPath = Paths.get(storeFolderPath).resolve(ROOT_DIRECTORY_NAME);
        initFolder(this.storeFolderPath);
    }

    @Override
    public String save(DataSet dataset) {
        if (alreadyExists(dataset)) {
            throw new DataSetAlreadyExistException(dataset.name);
        }
        DatasetDto dto = toDto(dataset);
        Path file = this.storeFolderPath.resolve(dto.id + FILE_EXTENSION);
        createFile(file);
        try {
            String jsonContent = objectMapper.writeValueAsString(dto);
            FileUtils.writeContent(file, jsonContent);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save " + file.toUri(), e);
        }
        return dto.id;
    }

    private boolean alreadyExists(DataSet dataset) {
        if (dataset.id != null) {
            // Not a new dataset
            return false;
        }
        try {
            DataSet byId = findById(dataset.name);
            return !byId.equals(DataSet.NO_DATASET);
        } catch (DataSetNotFoundException e) {
            return false;
        }
    }

    @Override
    public DataSet findById(String datasetId) {
        if (null == datasetId || datasetId.isBlank()) {
            return DataSet.NO_DATASET;
        }

        Path file = this.storeFolderPath.resolve(datasetId + FILE_EXTENSION);
        try {
            String content = FileUtils.readContent(file);
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            return fromDto(objectMapper.readValue(content, DatasetDto.class), attr.creationTime().toInstant());
        } catch (IOException | UncheckedIOException e) {
            throw new DataSetNotFoundException(datasetId, e);
        }
    }

    @Override
    public void removeById(String datasetId) {
        Path filePath = this.storeFolderPath.resolve(datasetId + FILE_EXTENSION);
        FileUtils.delete(filePath);
    }

    @Override
    public List<DataSet> findAll() {
        return FileUtils.doOnListFiles(storeFolderPath, (pathStream) ->
            pathStream
                .filter(Files::isRegularFile)
                .map(FileUtils::getNameWithoutExtension)
                .sorted(Comparator.naturalOrder())
                .map(this::findById)
                .collect(Collectors.toList())
        );
    }
}
