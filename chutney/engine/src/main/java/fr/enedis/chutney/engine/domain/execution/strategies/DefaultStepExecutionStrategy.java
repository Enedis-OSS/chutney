/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.strategies;

import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContext;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultStepExecutionStrategy implements StepExecutionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStepExecutionStrategy.class);

    public static final DefaultStepExecutionStrategy instance = new DefaultStepExecutionStrategy();

    private DefaultStepExecutionStrategy() {
    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public Status execute(ScenarioExecution scenarioExecution,
                          Step step,
                          ScenarioContext scenarioContext,
                          Map<String, Object> localContext,
                          StepExecutionStrategies strategies) {
        if (step.isParentStep()) {
            Iterator<Step> subStepsIterator = step.subSteps().iterator();
            step.beginExecution(scenarioExecution);
            Step currentRunningStep = step;
            try {
                Map<String, Object> context = new HashMap<>(scenarioContext);
                context.putAll(localContext);
                step.resolveName(context);
                Status childStatus = Status.RUNNING;
                while (subStepsIterator.hasNext() && childStatus != Status.FAILURE) {
                    currentRunningStep = subStepsIterator.next();
                    StepExecutionStrategy strategy = strategies.buildStrategyFrom(currentRunningStep);
                    childStatus = strategy.execute(scenarioExecution, currentRunningStep, scenarioContext, localContext, strategies);
                }
                return childStatus;
            } catch (RuntimeException e) {
                currentRunningStep.failure(e);
                LOGGER.warn("Intercepted exception!", e);
            } finally {
                step.endExecution(scenarioExecution);
            }
            return step.status();
        }

        return step.execute(scenarioExecution, scenarioContext, localContext);
    }
}
