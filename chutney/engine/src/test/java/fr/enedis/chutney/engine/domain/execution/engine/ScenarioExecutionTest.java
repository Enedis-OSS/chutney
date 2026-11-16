/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine;

import static fr.enedis.chutney.engine.domain.execution.RxBus.getInstance;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.command.PauseExecutionCommand;
import fr.enedis.chutney.engine.domain.execution.command.ResumeExecutionCommand;
import fr.enedis.chutney.engine.domain.execution.command.StopExecutionCommand;
import org.junit.jupiter.api.Test;

public class ScenarioExecutionTest {

    @Test
    public void events_should_change_execution_state() {

        // Init
        ScenarioExecution scenarioExecution = ScenarioExecution.createScenarioExecution(null);
        assertThat(scenarioExecution.hasToPause()).isFalse();
        assertThat(scenarioExecution.hasToStop()).isFalse();

        // Pause
        getInstance().post(new PauseExecutionCommand(scenarioExecution.executionId));
        await().atMost(1, SECONDS).untilAsserted(() -> {
                assertThat(scenarioExecution.hasToPause()).isTrue();
                assertThat(scenarioExecution.hasToStop()).isFalse();
            }
        );

        // Resume
        getInstance().post(new ResumeExecutionCommand(scenarioExecution.executionId));
        await().atMost(1, SECONDS).untilAsserted(() -> {
                assertThat(scenarioExecution.hasToPause()).isFalse();
                assertThat(scenarioExecution.hasToStop()).isFalse();
            }
        );

        // Stop
        getInstance().post(new StopExecutionCommand(scenarioExecution.executionId));
        await().atMost(1, SECONDS).untilAsserted(() -> {
                assertThat(scenarioExecution.hasToPause()).isFalse();
                assertThat(scenarioExecution.hasToStop()).isTrue();
            }
        );
    }

    @Test
    public void stop_should_interrupt_thread_only_while_interruption_is_allowed() {
        ScenarioExecution interruptible = ScenarioExecution.createScenarioExecution(null);
        interruptible.allowInterruption();
        getInstance().post(new StopExecutionCommand(interruptible.executionId));
        assertThat(Thread.interrupted()).isTrue();

        ScenarioExecution inTeardown = ScenarioExecution.createScenarioExecution(null);
        inTeardown.allowInterruption();
        inTeardown.forbidInterruption();
        getInstance().post(new StopExecutionCommand(inTeardown.executionId));
        assertThat(inTeardown.hasToStop()).isTrue();
        assertThat(Thread.interrupted()).isFalse();
    }
}
