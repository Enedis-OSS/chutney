/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import fr.enedis.chutney.action.TestActionTemplateFactory.ComplexAction;
import fr.enedis.chutney.action.TestActionTemplateFactory.Pojo;
import fr.enedis.chutney.action.TestActionTemplateFactory.TwoParametersAction;
import fr.enedis.chutney.action.TestActionTemplateFactory.ValidSimpleAction;
import fr.enedis.chutney.action.TypeBasedParameterResolver;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.ActionExecutionResult.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ActionTemplateV2Test {

    @Test
    public void simple_action_instantiation() {
        ActionTemplate actionTemplate = new ActionTemplateParserV2().parse(ValidSimpleAction.class).result();

        Action action = actionTemplate.create(Collections.emptyList());

        assertThat(action).isExactlyInstanceOf(ValidSimpleAction.class);
    }

    @Test
    public void action_with_parameters_instantiation_and_execution() {
        ActionTemplate actionTemplate = new ActionTemplateParserV2().parse(TwoParametersAction.class).result();
        String stringValue = UUID.randomUUID().toString();

        Action action = actionTemplate.create(Arrays.asList(
            new TypeBasedParameterResolver<>(String.class, p -> stringValue),
            new TypeBasedParameterResolver<>(int.class, p -> 0)
        ));

        assertThat(action).isExactlyInstanceOf(TwoParametersAction.class);

        ActionExecutionResult executionResult = action.execute();

        assertThat(executionResult.status).isEqualTo(Status.Success);
        assertThat(executionResult.outputs).containsOnly(entry("someString", stringValue), entry("someInt", 0));
    }

    @Test
    public void action_with_complex_parameters_instantiation_and_execution() {
        ActionTemplate actionTemplate = new ActionTemplateParserV2().parse(ComplexAction.class).result();
        String stringValue = UUID.randomUUID().toString();
        Pojo pojo = new Pojo("1", "2");
        Action action = actionTemplate.create(Arrays.asList(
            new TypeBasedParameterResolver<>(String.class, p -> stringValue),
            new TypeBasedParameterResolver<>(Pojo.class, p -> pojo)
        ));

        assertThat(action).isExactlyInstanceOf(ComplexAction.class);

        ActionExecutionResult executionResult = action.execute();

        assertThat(executionResult.status).isEqualTo(Status.Success);
        assertThat(executionResult.outputs).containsOnly(entry("someString", stringValue), entry("someObject", pojo));
    }
}
