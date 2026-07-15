/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra;

import static fr.enedis.chutney.config.ServerConfigurationValues.CONFIGURATION_FOLDER_SPRING_VALUE;
import static fr.enedis.chutney.tools.file.FileUtils.initFolder;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.enedis.chutney.security.api.AuthorizationMapper;
import fr.enedis.chutney.security.api.AuthorizationsDto;
import fr.enedis.chutney.security.domain.Authorizations;
import fr.enedis.chutney.server.core.domain.security.UserRoles;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JsonFileAuthorizations implements Authorizations {

    private static final String AUTHORIZATION_FILE_NAME = "authorization.json";
    private static final Path ROOT_DIRECTORY_NAME = Paths.get("roles");

    private final Path authorizationFilePath;

    private final ObjectMapper om = JsonMapper.builder()
        .findAndAddModules()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.ALWAYS))
        .build();

    JsonFileAuthorizations(@Value(CONFIGURATION_FOLDER_SPRING_VALUE) String configFolderPath) throws UncheckedIOException {
        Path storeFolderPath = Paths.get(configFolderPath).resolve(ROOT_DIRECTORY_NAME);
        this.authorizationFilePath = storeFolderPath.resolve(AUTHORIZATION_FILE_NAME);

        initFolder(storeFolderPath);
        initAuthorizationFile();
    }

    @Override
    public UserRoles read() {
        try {
            byte[] bytes = Files.readAllBytes(authorizationFilePath);
            return AuthorizationMapper.fromDto(om.readValue(bytes, AuthorizationsDto.class));
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot read authorization file : " + authorizationFilePath, e);
        } catch (tools.jackson.core.JacksonException e) {
            throw new UnsupportedOperationException("Cannot deserialize authorization file : " + authorizationFilePath, e);
        }
    }

    @Override
    public void save(UserRoles userRoles) {
        byte[] bytes = om.writeValueAsBytes(AuthorizationMapper.toDto(userRoles));
        try {
            Files.write(authorizationFilePath, bytes);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot write authorization file : " + authorizationFilePath, e);
        }
    }

    private void initAuthorizationFile() {
        if (Files.notExists(this.authorizationFilePath)) {
            save(UserRoles.builder().build());
        }
    }
}
