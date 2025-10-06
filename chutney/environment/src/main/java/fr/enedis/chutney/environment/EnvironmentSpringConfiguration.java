/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment;

import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.environment.api.target.EmbeddedTargetApi;
import fr.enedis.chutney.environment.api.variable.EnvironmentVariableApi;
import fr.enedis.chutney.server.core.domain.environment.UpdateEnvironmentHandler;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentSpringConfiguration {

    private static final String WORKSPACE_SPRING_VALUE = "${chutney.workspace:${user.home}/.chutney}";
    public static final String CONFIGURATION_FOLDER_SPRING_VALUE = "${chutney.configuration-folder:" + WORKSPACE_SPRING_VALUE + "/conf}";
    public static final String ENVIRONMENT_FOLDER ="/environment";


    @Bean
    EnvironmentConfiguration environmentConfiguration(@Value(CONFIGURATION_FOLDER_SPRING_VALUE) String storeFolderPath, List<UpdateEnvironmentHandler> updateEnvironmentHandlers) {
        return new EnvironmentConfiguration(storeFolderPath.concat(ENVIRONMENT_FOLDER), updateEnvironmentHandlers);
    }

    @Bean
    EmbeddedEnvironmentApi environmentEmbeddedApplication(EnvironmentConfiguration environmentConfiguration) {
        return environmentConfiguration.getEmbeddedEnvironmentApi();
    }

    @Bean
    EmbeddedTargetApi targetEmbeddedApplication(EnvironmentConfiguration environmentConfiguration) {
        return environmentConfiguration.getEmbeddedTargetApi();
    }

    @Bean
    EnvironmentVariableApi variableEmbeddedApplication(EnvironmentConfiguration environmentConfiguration) {
        return environmentConfiguration.getEmbeddedVariableApi();
    }
}
