/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.strategies;

import static fr.enedis.chutney.engine.domain.execution.report.Status.SUCCESS;

import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.EvaluationException;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.StepDataEvaluator;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContext;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import java.util.HashMap;
import java.util.Map;

public class IfStrategy implements StepExecutionStrategy {

    private static final String TYPE = "if";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Status execute(ScenarioExecution scenarioExecution, Step step, ScenarioContext scenarioContext, Map<String, Object> localContext, StepExecutionStrategies strategies) {
        final StepStrategyDefinition strategyDefinition = step.strategy().orElseThrow(
            () -> new IllegalArgumentException("Strategy definition cannot be empty")
        );

        final Object conditionObject = strategyDefinition.strategyProperties.getProperty("condition", Object.class);
        if (conditionObject == null) {
            throw new IllegalArgumentException("Property [condition] mandatory for if strategy");
        }
        final Boolean condition = getCondition(scenarioContext, conditionObject, step.dataEvaluator());

        final String conditionStatus = condition ? "step executed" : "step skipped";
        step.addInformation("Execution condition [" + conditionObject + "] = " + conditionStatus);

        if (condition) {
            return DefaultStepExecutionStrategy.instance.execute(scenarioExecution, step, scenarioContext, localContext, strategies);
        } else {
            Map<String, Object> context = new HashMap<>(scenarioContext);
            context.putAll(localContext);
            step.resolveName(context);
            step.success();
            skipAllSubSteps(step);
        }
        return SUCCESS;
    }

    private void skipAllSubSteps(Step step) {
        if (step.isParentStep()) {
            step.subSteps().forEach(subStep -> {
                subStep.addInformation("Step skipped");
                subStep.success();
                skipAllSubSteps(subStep);
            });
        }
    }

    private static Boolean getCondition(ScenarioContext scenarioContext, Object conditionObject, StepDataEvaluator evaluator) {
        if (conditionObject instanceof Boolean booleanCondition) {
            return booleanCondition;
        } else if (conditionObject instanceof String stringCondition) {
            try {
                return (Boolean) evaluator.evaluate(stringCondition, scenarioContext);
            } catch (EvaluationException | ClassCastException e) {
                throw new RuntimeException("Cannot evaluate execution condition: [" + conditionObject + "]. Error message: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Cannot evaluate execution condition: [" + conditionObject + "]. should be a boolean or a Spring Expression Language which return a boolean");
        }
    }
}
