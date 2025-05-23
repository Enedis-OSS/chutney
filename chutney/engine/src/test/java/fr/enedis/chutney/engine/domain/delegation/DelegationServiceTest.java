/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.delegation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.engine.DefaultStepExecutor;
import fr.enedis.chutney.engine.domain.execution.engine.StepExecutor;
import com.google.common.collect.Lists;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DelegationServiceTest {

    public static Object[] parametersForShould_return_local_executor() {
        TargetImpl targetWithNoName = TargetImpl.builder()
            .withUrl("proto://host:12345")
            .build();
        TargetImpl targetWithNoAgents = TargetImpl.builder().copyOf(targetWithNoName)
            .withName("name")
            .build();
        return new Object[][]{
            {empty()},
            {of(targetWithNoName)},
            {of(targetWithNoAgents)},
        };
    }

    @ParameterizedTest
    @MethodSource("parametersForShould_return_local_executor")
    public void should_return_local_executor(Optional<Target> target) {
        // Given
        DelegationService delegationService = new DelegationService(mock(DefaultStepExecutor.class), mock(DelegationClient.class));

        // When
        StepExecutor actual = delegationService.findExecutor(target);

        // Then
        assertThat(actual).as("StepExecutor").isInstanceOf(DefaultStepExecutor.class);
    }

    @Test
    public void should_return_remote_executor() {
        // Given
        DelegationService delegationService = new DelegationService(mock(DefaultStepExecutor.class), mock(DelegationClient.class));
        TargetImpl targetWithAgent = TargetImpl.builder()
            .withName("name")
            .withAgents(Lists.newArrayList(new NamedHostAndPort("name", "host", 12345)))
            .build();

        // When
        StepExecutor actual = delegationService.findExecutor(of(targetWithAgent));

        // Then
        assertThat(actual).as("StepExecutor").isInstanceOf(RemoteStepExecutor.class);
        assertThat(targetWithAgent.agents).isEmpty();
    }

}
