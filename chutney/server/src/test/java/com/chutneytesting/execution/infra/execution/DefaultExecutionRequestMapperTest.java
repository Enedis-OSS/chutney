/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.execution.infra.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chutneytesting.agent.domain.explore.CurrentNetworkDescription;
import com.chutneytesting.engine.api.execution.ExecutionRequestDto;
import com.chutneytesting.environment.api.environment.EmbeddedEnvironmentApi;
import com.chutneytesting.environment.api.environment.dto.EnvironmentDto;
import com.chutneytesting.environment.api.target.EmbeddedTargetApi;
import com.chutneytesting.scenario.domain.gwt.GwtScenario;
import com.chutneytesting.scenario.domain.gwt.GwtStep;
import com.chutneytesting.scenario.domain.gwt.GwtStepImplementation;
import com.chutneytesting.scenario.domain.gwt.GwtTestCase;
import com.chutneytesting.scenario.domain.gwt.Strategy;
import com.chutneytesting.server.core.domain.dataset.DataSet;
import com.chutneytesting.server.core.domain.execution.ExecutionRequest;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadataImpl;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class DefaultExecutionRequestMapperTest {

    private final EmbeddedTargetApi embeddedTargetApi = mock(EmbeddedTargetApi.class);
    private final EmbeddedEnvironmentApi embeddedEnvironmentApi = mock(EmbeddedEnvironmentApi.class);
    private final CurrentNetworkDescription currentNetworkDescription = mock(CurrentNetworkDescription.class);

    private final DefaultExecutionRequestMapper sut = new DefaultExecutionRequestMapper(embeddedTargetApi, embeddedEnvironmentApi, currentNetworkDescription);

    @Test
    public void should_map_test_case_to_execution_request() {
        // Given
        String envName = "env";
        EnvironmentDto env = new EnvironmentDto(envName);
        when(embeddedEnvironmentApi.getEnvironment(envName)).thenReturn(env);
        GwtTestCase gwtTestCase = getGwtTestCase();
        DataSet dataset = DataSet.builder().withName("ds").withConstants(Map.of("A", "B")).build();
        ExecutionRequest request = new ExecutionRequest(gwtTestCase, envName, "", dataset);

        // When
        ExecutionRequestDto executionRequestDto = sut.toDto(request);

        // Then
        assertThat(executionRequestDto.scenario).isNotNull();
        assertThat(executionRequestDto.scenario.name).isEqualTo("root step");
        assertThat(executionRequestDto.scenario.steps.getFirst().name).isEqualTo("context-put name");
        assertThat(executionRequestDto.scenario.steps.getFirst().inputs).containsKey("someID");
        assertThat(executionRequestDto.environment).isNotNull();
        assertThat(executionRequestDto.environment.name()).isEqualTo(envName);
        assertThat(executionRequestDto.dataset.constants).isEqualTo(dataset.constants);
    }

    private GwtTestCase getGwtTestCase() {
        return GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder().withTitle("root step").build())
            .withScenario(GwtScenario.builder()
                .withTitle("root step")
                .withGivens(List.of(
                    GwtStep.builder()
                        .withDescription("context-put name")
                        .withStrategy(new Strategy("retry-with-timeout", Map.of("timeOut", "20 min", "retryDelay", "1 min")))
                        .withImplementation(new GwtStepImplementation(
                            "context-put",
                            null,
                            Map.of("someID", "${'prt' + #generate().uuid()}"),
                            Map.of("someXML", "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n <project xmlns=\"http://maven.apache.org/POM/4.0.0\" \n xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"> \n  ...</project>"),
                            Collections.emptyMap(),
                            null
                        ))
                        .build()
                ))
                .withWhen(
                    GwtStep.builder()
                        .withDescription("debug name")
                        .withImplementation(new GwtStepImplementation(
                            "debug",
                            null,
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            null
                        ))
                        .build()
                )
                .withThens(List.of(
                    GwtStep.builder()
                        .withDescription("sleep name")
                        .withImplementation(new GwtStepImplementation(
                            "sleep",
                            null,
                            Map.of("duration", "3 sec"),
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            null
                        ))
                        .build()
                ))
                .build())
            .build();
    }

}
