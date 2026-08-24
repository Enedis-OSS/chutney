/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.execution.domain.GwtScenarioMarshaller;
import fr.enedis.chutney.scenario.domain.gwt.GwtScenario;
import fr.enedis.chutney.scenario.domain.gwt.GwtStep;
import fr.enedis.chutney.scenario.domain.gwt.GwtStepImplementation;
import fr.enedis.chutney.scenario.domain.gwt.Strategy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

public class GwtScenarioMapperTest {

    private static final GwtScenarioMarshaller marshaller = new GwtScenarioMapper();

    @Test
    public void should_deserialize_a_raw_scenario_with_x$ref() {
        // Given raw test v2.1 with x-$ref
        String rawScenario = Files.contentOf(new File(GwtScenarioMapperTest.class.getResource("/raw_scenarios/raw_scenario_json_with_x-$ref.json").getPath()), StandardCharsets.UTF_8);

        // When: deserialize into GwtScenario
        GwtScenario actualScenario = marshaller.deserialize("a title", "a description", rawScenario);

        //Then:
        assertThat(actualScenario.givens.size()).isEqualTo(2);
        assertThat(actualScenario.givens.getFirst().xRef).hasValue("common/frag1.icefrag.json");
        assertThat(actualScenario.givens.get(1).implementation.get().xRef).isEqualTo("common/frag2.icefrag.json");

    }

    @Test
    public void should_deserialize_a_raw_scenario() {
        // Given raw
        String rawScenario = Files.contentOf(new File(GwtScenarioMapperTest.class.getResource("/raw_scenarios/scenario_executable.v2.1.json").getPath()), StandardCharsets.UTF_8);

        // When
        GwtScenario actualScenario = marshaller.deserialize("a title", "a description", rawScenario);

        //Then:
        assertThat(actualScenario.givens.size()).isEqualTo(1);
        assertThat(actualScenario.givens.getFirst().implementation.get().inputs).containsEntry("fake_param", "fake_value");
        assertThat(actualScenario.givens.getFirst().implementation.get().outputs).containsEntry("fake_output", "fake_output_value");
        assertThat(actualScenario.givens.getFirst().implementation.get().validations).containsEntry("fake_validation", "${true}");
    }

    @Test
    public void should_serialize_scenario_keys_in_gwt_order_and_not_alphabetically() {
        // Given a scenario whose GWT order differs from the alphabetical one
        GwtScenario scenario = GwtScenario.builder()
            .withGivens(List.of(GwtStep.builder().withDescription("a given").build()))
            .withWhen(GwtStep.builder().withDescription("the when").build())
            .withThens(List.of(GwtStep.builder().withDescription("a then").build()))
            .build();

        // When
        String json = marshaller.serialize(scenario);

        // Then: givens, when, thens and not the alphabetical givens, thens, when
        assertThat(json.indexOf("\"givens\"")).isLessThan(json.indexOf("\"when\""));
        assertThat(json.indexOf("\"when\"")).isLessThan(json.indexOf("\"thens\""));
    }

    @Test
    public void should_serialize_step_keys_in_declaration_order_and_not_alphabetically() {
        // Given a step declaring sub steps, an implementation and a strategy
        GwtStep step = GwtStep.builder()
            .withDescription("a given")
            .withSubSteps(List.of(GwtStep.builder().withDescription("a sub step").build()))
            .withImplementation(new GwtStepImplementation("success", "a target", Map.of("input", "value"), Map.of(), Map.of(), ""))
            .withStrategy(new Strategy("retry-with-timeout", Map.of("timeOut", "1 s")))
            .build();
        GwtScenario scenario = GwtScenario.builder()
            .withWhen(step)
            .build();

        // When
        String json = marshaller.serialize(scenario);

        // Then: description, subSteps, implementation, strategy and not the alphabetical description, implementation, strategy, subSteps
        assertThat(json.indexOf("\"description\"")).isLessThan(json.indexOf("\"subSteps\""));
        assertThat(json.indexOf("\"subSteps\"")).isLessThan(json.indexOf("\"implementation\""));
        assertThat(json.indexOf("\"implementation\"")).isLessThan(json.indexOf("\"strategy\""));
    }

}
