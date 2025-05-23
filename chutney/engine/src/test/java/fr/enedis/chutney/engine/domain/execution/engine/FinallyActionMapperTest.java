/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.StepDefinition;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FinallyActionMapperTest {

    private final FinallyActionMapper mapper = new FinallyActionMapper();

    @Test
    public void upright_finally_action_copy() {
        FinallyAction finallyAction = FinallyAction.Builder
            .forAction("test-action", "action name")
            .withTarget(TargetImpl.builder()
                .withName("test-target")
                .withUrl("proto://host:12345")
                .build())
            .withInput("test-input", "test")
            .withValidation("test-validation", true)
            .withStrategyType("strategyType")
            .withStrategyProperties(Map.of("param", "value"))
            .build();

        StepDefinition stepDefinition = mapper.toStepDefinition(finallyAction);

        assertThat(stepDefinition.type).isEqualTo("test-action");
        assertThat(stepDefinition.name).isEqualTo("action name");
        assertThat(stepDefinition.inputs()).containsOnly(entry("test-input", "test"));
        assertThat(stepDefinition.validations).containsOnly(entry("test-validation", true));
        assertThat(stepDefinition.getTarget()).isPresent();
        Target targetCopy = stepDefinition.getTarget().get();
        assertThat(targetCopy.name()).isEqualTo("test-target");
        assertThat(targetCopy.uri().toString()).isEqualTo("proto://host:12345");
        assertThat(targetCopy.user()).isEmpty();
        assertThat(targetCopy.userPassword()).isEmpty();
        assertThat(stepDefinition.getStrategy()).hasValueSatisfying(s -> {
            assertThat(s.type).isEqualTo("strategyType");
            assertThat(s.strategyProperties).contains(entry("param", "value"));
        });
    }
}
