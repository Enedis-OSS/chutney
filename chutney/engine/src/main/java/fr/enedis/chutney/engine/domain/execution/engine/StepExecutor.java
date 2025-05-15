/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;

public interface StepExecutor {

    void execute(ScenarioExecution scenarioExecution, Target target, Step step);

}
