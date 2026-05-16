/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.infra.storage.plugins.linkifier;

import static fr.enedis.chutney.config.ServerConfigurationValues.CONFIGURATION_FOLDER_SPRING_VALUE;
import static fr.enedis.chutney.tools.file.FileUtils.initFolder;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.enedis.chutney.design.domain.plugins.linkifier.Linkifier;
import fr.enedis.chutney.design.domain.plugins.linkifier.Linkifiers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LinkifierFileRepository implements Linkifiers {

    private static final Path ROOT_DIRECTORY_NAME = Paths.get("plugins");
    private static final String LINKIFIER_FILE = "linkifiers.json";

    private final Path storeFolderPath;
    private final Path resolvedFilePath;

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY))
        .build();

    LinkifierFileRepository(@Value(CONFIGURATION_FOLDER_SPRING_VALUE) String storeFolderPath) throws UncheckedIOException {
        this.storeFolderPath = Paths.get(storeFolderPath).resolve(ROOT_DIRECTORY_NAME);
        this.resolvedFilePath = this.storeFolderPath.resolve(LINKIFIER_FILE);
        initFolder(this.storeFolderPath);
    }

    @Override
    public List<Linkifier> getAll() {
        return getAll(resolvedFilePath);
    }

    private List<Linkifier> getAll(Path filePath) {
        return readFile(filePath).entrySet().stream()
            .map(e -> this.fromDto(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public Linkifier add(Linkifier linkifier) {
        Map<String, LinkifierDto> linkifiers = readDefaultFile();
        linkifiers.put(linkifier.id, toDto(linkifier));
        writeOnDisk(resolvedFilePath, linkifiers);
        return linkifier;
    }

    @Override
    public void remove(String id) {
        Map<String, LinkifierDto> linkifiers = readDefaultFile();
        linkifiers.remove(id);
        writeOnDisk(resolvedFilePath, linkifiers);
    }

    private Map<String, LinkifierDto> readDefaultFile() {
        return readFile(resolvedFilePath);
    }

    private Map<String, LinkifierDto> readFile(Path filePath) {
        Map<String, LinkifierDto> linkifiers = new HashMap<>();
        try {
            if (Files.exists(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                linkifiers.putAll(objectMapper.readValue(bytes, new TypeReference<HashMap<String, LinkifierDto>>() {}));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read configuration file: " + filePath, e);
        }

        return linkifiers;
    }

    private void writeOnDisk(Path filePath, Map<String, LinkifierDto> linkifiers) {
        byte[] bytes = objectMapper.writeValueAsBytes(linkifiers);
        try {
            Files.write(filePath, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write in configuration directory: " + storeFolderPath, e);
        }
    }


    public Linkifier fromDto(String id, LinkifierDto dto) {
        return new Linkifier(dto.pattern, dto.link, id);
    }

    public LinkifierDto toDto(Linkifier linkifier) {
        return new LinkifierDto(linkifier.pattern, linkifier.link);
    }
}
