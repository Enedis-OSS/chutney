/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira;

import fr.enedis.chutney.jira.api.JiraXrayEmbeddedApi;
import fr.enedis.chutney.jira.domain.JiraRepository;
import fr.enedis.chutney.jira.domain.JiraXrayClientFactory;
import fr.enedis.chutney.jira.domain.JiraXrayService;
import fr.enedis.chutney.jira.infra.JiraFileRepository;
import fr.enedis.chutney.jira.infra.JiraXrayFactoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JiraSpringConfiguration {
    private static final String WORKSPACE_SPRING_VALUE = "${chutney.workspace:${user.home}/.chutney}";
    public static final String CONFIGURATION_FOLDER_SPRING_VALUE = "${chutney.workspace.configuration-folder:" + WORKSPACE_SPRING_VALUE + "/conf}";

    public static final String JIRA_CONFIGURATION_FOLDER_SPRING_VALUE = "${chutney.jira.configuration-folder:" + CONFIGURATION_FOLDER_SPRING_VALUE + "/jira}";

    // api Bean
    @Bean
    JiraXrayEmbeddedApi jiraXrayEmbeddedApi(JiraXrayService jiraXrayService) {
        return new JiraXrayEmbeddedApi(jiraXrayService);
    }

    // domain Bean
    @Bean
    JiraXrayService jiraXrayService(JiraRepository jiraRepository, JiraXrayClientFactory jiraXrayFactory) {
        return new JiraXrayService(jiraRepository, jiraXrayFactory);
    }

    // infra Bean
    @Bean
    JiraXrayClientFactory jiraXrayFactory() {
        return new JiraXrayFactoryImpl();
    }

    @Bean
    JiraRepository jiraFileRepository(@Value(JIRA_CONFIGURATION_FOLDER_SPRING_VALUE) String storeFolderPath) {
        return new JiraFileRepository(storeFolderPath);
    }
}
