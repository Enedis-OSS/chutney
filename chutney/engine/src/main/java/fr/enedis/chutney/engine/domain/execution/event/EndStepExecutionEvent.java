/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.event;

import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;

public class EndStepExecutionEvent implements Event{
    public final ScenarioExecution scenarioExecution;
    public final Step step;

    public EndStepExecutionEvent(ScenarioExecution scenarioExecution, Step step) {
        this.scenarioExecution = scenarioExecution;
        this.step = step;
    }

    @Override
    public long executionId() {
        return scenarioExecution.executionId;
    }
}
