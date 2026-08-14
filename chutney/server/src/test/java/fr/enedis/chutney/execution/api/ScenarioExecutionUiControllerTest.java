/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.dataset.domain.DataSetRepository;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.security.infra.SpringUserService;
import fr.enedis.chutney.server.core.domain.execution.FailedExecutionAttempt;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngine;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngineAsync;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ScenarioExecutionUiControllerTest {

    @Test
    void should_return_created_execution_id_when_engine_fails_before_starting() {
        ScenarioExecutionEngineAsync executionEngineAsync = mock(ScenarioExecutionEngineAsync.class);
        TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
        SpringUserService userService = mock(SpringUserService.class);
        GwtTestCase testCase = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder().withId("123100").withTitle("scenario").build())
            .build();
        when(testCaseRepository.findExecutableById("123100")).thenReturn(of(testCase));
        when(userService.currentUserId()).thenReturn("user");
        when(executionEngineAsync.execute(any()))
            .thenThrow(new FailedExecutionAttempt(new IllegalArgumentException("Target not found"), 13L, "scenario"));
        ScenarioExecutionUiController sut = new ScenarioExecutionUiController(
            mock(ScenarioExecutionEngine.class),
            executionEngineAsync,
            testCaseRepository,
            mock(ObjectMapper.class),
            userService,
            mock(DataSetRepository.class),
            mock(ScenarioExecutionReportMapper.class),
            mock(EmbeddedEnvironmentApi.class)
        );

        String executionId = sut.executeScenarioAsyncWithExecutionParameters("123100", "DEMO", null);

        assertThat(executionId).isEqualTo("13");
    }
}
