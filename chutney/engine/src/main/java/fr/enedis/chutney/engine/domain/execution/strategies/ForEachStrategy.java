/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.strategies;

import static java.util.Collections.emptyList;

import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.StepDefinition;
import fr.enedis.chutney.engine.domain.execution.StepDefinitionBuilder;
import fr.enedis.chutney.engine.domain.execution.engine.StepExecutor;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.StepDataEvaluator;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContext;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;

public class ForEachStrategy implements StepExecutionStrategy {

    @Override
    public String getType() {
        return "for";
    }

    @Override
    public Status execute(ScenarioExecution scenarioExecution,
                          Step step,
                          ScenarioContext scenarioContext,
                          Map<String, Object> localContext,
                          StepExecutionStrategies strategies) {

        StepStrategyDefinition strategyDefinition = step.strategy().orElseThrow(
            () -> new IllegalArgumentException("Strategy definition cannot be empty")
        );

        List<Map<String, Object>> dataset = getDataset(step, scenarioContext, strategyDefinition, step.dataEvaluator());

        if (executeRetryWithDataset(scenarioExecution, step, scenarioContext, localContext, strategies, dataset)) {
            return step.status();
        }

        final String indexName = computeIndexName(strategyDefinition);
        step.beginExecution(scenarioExecution);
        replaceIndexInStepName(step, scenarioContext, localContext);
        if (step.isParentStep()) {
            executeParentStep(scenarioExecution, step, scenarioContext, localContext, strategies, dataset, indexName);
        } else {
            executeSubSteps(scenarioExecution, step, scenarioContext, localContext, dataset, indexName);
        }
        step.endExecution(scenarioExecution);
        return step.status();
    }

    private String computeIndexName(StepStrategyDefinition strategyDefinition) {
        return (String) Optional.ofNullable(strategyDefinition.strategyProperties.get("index")).orElse("i");
    }

    private void replaceIndexInStepName(Step step, ScenarioContext scenarioContext, Map<String, Object> localContext) {
        Map<String, Object> context = new HashMap<>(scenarioContext);
        context.putAll(localContext);
        step.resolveName(context);
    }

    private void executeParentStep(ScenarioExecution scenarioExecution, Step step, ScenarioContext scenarioContext, Map<String, Object> localContext, StepExecutionStrategies strategies, List<Map<String, Object>> dataset, String indexName) {
        AtomicInteger index = new AtomicInteger(0);
        List<Step> subSteps = List.copyOf(step.subSteps());
        step.removeStepExecution();

        List<Pair<Step, Map<String, Object>>> iterations = dataset.stream()
            .map(iterationContext -> buildParentIteration(indexName, index.getAndIncrement(), step, subSteps, iterationContext))
            .peek(p -> step.addStepExecution(p.getLeft()))
            .toList();

        iterations.forEach(it -> {
            Map<String, Object> mergedContext = new HashMap<>(localContext);
            mergedContext.putAll(it.getRight());
            DefaultStepExecutionStrategy.instance.execute(scenarioExecution, it.getLeft(), scenarioContext, mergedContext, strategies);
        });
    }

    private void executeSubSteps(ScenarioExecution scenarioExecution, Step step, ScenarioContext scenarioContext, Map<String, Object> localContext, List<Map<String, Object>> dataset, String indexName) {
        AtomicInteger index = new AtomicInteger(0);
        List<Pair<Step, Map<String, Object>>> iterations = dataset.stream()
            .map(iterationContext -> buildIteration(indexName, index.getAndIncrement(), step, iterationContext))
            .peek(e -> step.addStepExecution(e.getKey()))
            .toList();

        iterations.forEach(it -> {
            Map<String, Object> mergedContext = new HashMap<>(localContext);
            mergedContext.putAll(it.getRight());
            it.getLeft().execute(scenarioExecution, scenarioContext, mergedContext);
        });
    }

    private boolean executeRetryWithDataset(ScenarioExecution scenarioExecution, Step step, ScenarioContext scenarioContext, Map<String, Object> localContext, StepExecutionStrategies strategies, List<Map<String, Object>> dataset) {
        if (step.isForStrategyApplied()) {
            step.beginExecution(scenarioExecution);
            IntStream.range(0, step.subSteps().size()).forEach(i -> {
                    Map<String, Object> mergedContext = new HashMap<>(localContext);
                    mergedContext.putAll(dataset.get(i));
                    var stepToExecute = step.subSteps().get(i);
                    DefaultStepExecutionStrategy.instance.execute(scenarioExecution, stepToExecute, scenarioContext, mergedContext, strategies);
                }
            );

            step.endExecution(scenarioExecution);
            return true;
        } else {
            step.setIsForStrategyApplied(true);
            return false;
        }
    }

    private static List<Map<String, Object>> getDataset(Step step, ScenarioContext scenarioContext, StepStrategyDefinition strategyDefinition, StepDataEvaluator evaluator) {
        List<Map<String, Object>> dataset = (List<Map<String, Object>>) step.dataEvaluator().evaluate(strategyDefinition.strategyProperties.get("dataset"), scenarioContext);
        if (dataset.isEmpty()) {
            throw new IllegalArgumentException("Step iteration cannot have empty dataset");
        }

        return dataset.stream()
            .map(iterationData -> iterationData.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), evaluator.evaluate(e.getValue(), scenarioContext)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .toList();
    }

    private Pair<Step, Map<String, Object>> buildParentIteration(String indexName, Integer index, Step step, List<Step> subSteps, Map<String, Object> iterationContext) {

        StepDefinition newDef = iterationDefinition(indexName, index, step.definition(), new StepStrategyDefinition("", new StrategyProperties()));
        List<Step> newSubSteps = subSteps.stream().map(
            subStep -> buildIterationDefinition(indexName, index, subStep.dataEvaluator(), subStep.definition(), subStep.executor(), subStep.subSteps(), subStep.strategy().orElse(new StepStrategyDefinition("", new StrategyProperties())))
        ).toList();

        return Pair.of(
            new Step(step.dataEvaluator(), newDef, step.executor(), newSubSteps),
            iterationContext
        );
    }

    private Pair<Step, Map<String, Object>> buildIteration(String indexName, Integer index, Step step, Map<String, Object> iterationContext) {
        return Pair.of(
            new Step(step.dataEvaluator(), iterationDefinition(indexName, index, step.definition(), new StepStrategyDefinition("", new StrategyProperties())), step.executor(), emptyList()),
            iterationContext
        );
    }

    private Step buildIterationDefinition(String indexName, Integer index, StepDataEvaluator dataEvaluator, StepDefinition definition, StepExecutor executor, List<Step> subStep, StepStrategyDefinition strategy) {
        StepDefinition iterationDefinition = iterationDefinition(indexName, index, definition, Optional.ofNullable(strategy).orElse(new StepStrategyDefinition("", new StrategyProperties())));
        return new Step(dataEvaluator, iterationDefinition, executor, subStep.stream().map(step -> buildIterationDefinition(indexName, index, step.dataEvaluator(), step.definition(), step.executor(), step.subSteps(), step.strategy().orElse(null))).collect(Collectors.toList())); // We need this list to be mutable because of the clear in step.removeStepExecution()
    }

    private StepDefinition iterationDefinition(String indexName, Integer index, StepDefinition definition, StepStrategyDefinition strategyDefinition) {
        return StepDefinitionBuilder.copyFrom(definition)
            .withName(index(indexName, index, definition.name))
            .withInputs(index(indexName, index, definition.inputs()))
            .withOutputs(index(indexName, index, definition.outputs))
            .withValidations(index(indexName, index, definition.validations))
            .withStrategy(strategyDefinition)
            .withSteps(definition
                .steps
                .stream()
                .map(subStep -> iterationDefinition(indexName, index, subStep, subStep.getStrategy().orElse(new StepStrategyDefinition("", new StrategyProperties()))))
                .toList())
            .build();
    }

    private String index(String indexName, Integer index, String string) {
        return string.replace("<" + indexName + ">", index.toString());
    }

    private Map<String, Object> index(String indexName, Integer index, Map<String, Object> inputs) {
        return inputs.entrySet().stream()
            .collect(Collectors.toMap(
                e -> index(indexName, index, e.getKey()),
                e -> index(indexName, index, e.getValue())
            ));
    }

    private List<Object> index(String indexName, Integer index, List<Object> inputs) {
        return inputs.stream()
            .map(e -> index(indexName, index, e))
            .toList();
    }

    private Object index(String indexName, Integer index, Object value) {
        if (value instanceof Map) {
            return index(indexName, index, (Map) value);
        }

        if (value instanceof List) {
            return index(indexName, index, (List) value);
        }

        if (value instanceof String) {
            return index(indexName, index, (String) value);
        }

        return value;

    }
}
