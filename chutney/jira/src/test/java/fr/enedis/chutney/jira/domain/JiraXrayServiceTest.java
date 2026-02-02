/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.jira.domain.exception.NoJiraConfigurationException;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class JiraXrayServiceTest {

    @Test
    void update_jira_configuration_before_each_api_calls() throws Exception {
        // Given
        var jiraRepository = mock(JiraRepository.class);
        var initialJiraConfiguration = new JiraServerConfiguration("", "", "", null, null, null);
        when(jiraRepository.loadServerConfiguration()).thenReturn(initialJiraConfiguration);

        var jiraXrayClientFactory = mock(JiraXrayClientFactory.class);
        var metrics = mock(ChutneyMetrics.class);
        var jiraXrayApi = mock(JiraXrayApi.class);
        when(jiraXrayClientFactory.create(any(), any())).thenReturn(jiraXrayApi);

        JiraXrayService sut = new JiraXrayService(jiraRepository, jiraXrayClientFactory, metrics);
        Field jiraConfigurationField = sut.getClass().getDeclaredField("jiraServerConfiguration");
        jiraConfigurationField.setAccessible(true);

        // When empty initial configuration
        assertThatThrownBy(() -> sut.getTestExecutionScenarios("NOP-123"))
            .isInstanceOf(NoJiraConfigurationException.class);

        assertThat(jiraConfigurationField.get(sut)).isEqualTo(initialJiraConfiguration);

        // When new configuration
        var newJiraConfiguration = new JiraServerConfiguration("http://jira.server", "", "", null, null, null);
        when(jiraRepository.loadServerConfiguration()).thenReturn(newJiraConfiguration);
        sut.getTestExecutionScenarios("NOP-123");

        assertThat(jiraConfigurationField.get(sut)).isEqualTo(newJiraConfiguration);
        verify(jiraXrayApi).getTestExecutionScenarios("NOP-123");
    }
}
