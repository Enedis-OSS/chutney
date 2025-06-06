/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.execution.strategies.StepStrategyDefinition;
import java.util.List;
import java.util.Map;

public class StepDefinitionBuilder {

    private String name;
    private Target target;
    private String type;
    private StepStrategyDefinition strategy;
    private Map<String, Object> inputs;
    private List<StepDefinition> steps;
    private Map<String, Object> outputs;
    private Map<String, Object> validations;

    public StepDefinitionBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public StepDefinitionBuilder withTarget(Target target) {
        this.target = target;
        return this;
    }

    public StepDefinitionBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public StepDefinitionBuilder withStrategy(StepStrategyDefinition strategy) {
        this.strategy = strategy;
        return this;
    }

    public StepDefinitionBuilder withInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
        return this;
    }

    public StepDefinitionBuilder withSteps(List<StepDefinition> steps) {
        this.steps = steps;
        return this;
    }

    public StepDefinitionBuilder withOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
        return this;
    }

    public StepDefinitionBuilder withValidations(Map<String, Object> validations) {
        this.validations = validations;
        return this;
    }

    public StepDefinition build() {
        return new StepDefinition(name, target, type, strategy, inputs, steps, outputs, validations);
    }

    public static StepDefinitionBuilder copyFrom(StepDefinition definition) {
        StepDefinitionBuilder builder = new StepDefinitionBuilder();
        builder.withName(definition.name);
        builder.withTarget(definition.getTarget().orElse(null));
        builder.withType(definition.type);
        builder.withStrategy(definition.getStrategy().orElse(null));
        builder.withInputs(definition.inputs());
        builder.withSteps(definition.steps);
        builder.withOutputs(definition.outputs);
        builder.withValidations(definition.validations);
        return builder;
    }
}
