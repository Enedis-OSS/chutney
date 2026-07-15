/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.infra;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.server.core.domain.admin.Backupable;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EnvironmentBackupRepository implements Backupable {

    private final EnvironmentApi embeddedEnvironmentApi;

    private final ObjectMapper om = JsonMapper.builder()
        .findAndAddModules()
        .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY))
        .build();

    public EnvironmentBackupRepository(EmbeddedEnvironmentApi embeddedEnvironmentApi) {
        this.embeddedEnvironmentApi = embeddedEnvironmentApi;
    }

    @Override
    public void backup(OutputStream outputStream) {
        try (ZipOutputStream zipOutPut = new ZipOutputStream(new BufferedOutputStream(outputStream, 4096))) {
            for (EnvironmentDto env : embeddedEnvironmentApi.listEnvironments()) {
                zipOutPut.putNextEntry(new ZipEntry(env.name + ".json"));
                om.writeValue(zipOutPut, env);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public String name() {
        return "environments";
    }
}
