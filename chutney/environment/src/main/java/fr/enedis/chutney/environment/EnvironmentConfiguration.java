/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment;

import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.environment.api.target.EmbeddedTargetApi;
import fr.enedis.chutney.environment.api.variable.EmbeddedVariableApi;
import fr.enedis.chutney.environment.domain.Environment;
import fr.enedis.chutney.environment.domain.EnvironmentRepository;
import fr.enedis.chutney.environment.domain.EnvironmentService;
import fr.enedis.chutney.environment.infra.JsonFilesEnvironmentRepository;
import fr.enedis.chutney.server.core.domain.environment.UpdateEnvironmentHandler;
import java.util.List;

public class EnvironmentConfiguration {

    public static final String DEFAULT_ENV_NAME = "DEFAULT";
    private final EnvironmentRepository environmentRepository;
    private final EmbeddedEnvironmentApi environmentApi;
    private final EmbeddedTargetApi targetApi;
    private final EmbeddedVariableApi variableApi;

    public EnvironmentConfiguration(String storeFolderPath) {
        this(storeFolderPath, null);
    }

    public EnvironmentConfiguration(String storeFolderPath, List<UpdateEnvironmentHandler> updateEnvironmentHandlers) {
        this.environmentRepository = createEnvironmentRepository(storeFolderPath);
        EnvironmentService environmentService = createEnvironmentService(environmentRepository, updateEnvironmentHandlers);
        this.environmentApi = new EmbeddedEnvironmentApi(environmentService);
        this.targetApi = new EmbeddedTargetApi(environmentService);
        this.variableApi = new EmbeddedVariableApi(environmentService);

        createDefaultEnvironment(environmentService);
    }

    private void createDefaultEnvironment(EnvironmentService environmentService) {
        if (environmentRepository.listNames().isEmpty()) {
            environmentService.createEnvironment(Environment.builder().withName(DEFAULT_ENV_NAME).build());
        }
    }

    private EnvironmentRepository createEnvironmentRepository(String storeFolderPath) {
        return new JsonFilesEnvironmentRepository(storeFolderPath);
    }

    private EnvironmentService createEnvironmentService(EnvironmentRepository environmentRepository, List<UpdateEnvironmentHandler> updateEnvironmentHandlers) {
        return new EnvironmentService(environmentRepository, updateEnvironmentHandlers);
    }

    public EmbeddedEnvironmentApi getEmbeddedEnvironmentApi() {
        return environmentApi;
    }

    public EmbeddedTargetApi getEmbeddedTargetApi() {
        return targetApi;
    }

    public EmbeddedVariableApi getEmbeddedVariableApi() {
        return variableApi;
    }
}
