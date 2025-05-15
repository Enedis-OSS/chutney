/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.infra;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestActionTemplateFactory.TestAction;
import fr.enedis.chutney.action.TestActionTemplateFactory.TestAction3;
import fr.enedis.chutney.action.domain.ActionInstantiationFailureException;
import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.ActionTemplateParser;
import fr.enedis.chutney.action.domain.ParsingError;
import fr.enedis.chutney.action.domain.ResultOrError;
import fr.enedis.chutney.action.domain.UnresolvableActionParameterException;
import fr.enedis.chutney.action.domain.parameter.Parameter;
import fr.enedis.chutney.action.domain.parameter.ParameterResolver;
import fr.enedis.chutney.action.spi.Action;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DefaultActionTemplateLoaderTest {

    @Test
    public void load_from_test_file() {
        DefaultActionTemplateLoader<TestAction> actionTemplateLoader = new DefaultActionTemplateLoader<>(
            "test.actions",
            TestAction.class,
            new TestActionTemplateParser());

        assertThat(actionTemplateLoader.load())
            .as("Loaded ActionTemplates")
            .hasSize(2)
            .extracting(ActionTemplate::identifier).containsExactlyInAnyOrder("TestAction1", "TestAction2");

    }

    static class TestActionTemplateParser implements ActionTemplateParser<TestAction> {

        @Override
        public ResultOrError<ActionTemplate, ParsingError> parse(Class<? extends TestAction> actionClass) {
            if (TestAction3.class.equals(actionClass)) {
                return ResultOrError.error(new ParsingError(actionClass, "test error"));
            }
            ActionTemplate actionTemplate = new ActionTemplate() {

                @Override
                public String identifier() {
                    return actionClass.getSimpleName();
                }

                @Override
                public Class<?> implementationClass() {
                    return actionClass;
                }

                @Override
                public Set<Parameter> parameters() {
                    return Collections.emptySet();
                }

                @Override
                public Action create(List<ParameterResolver> parameterResolvers) throws UnresolvableActionParameterException, ActionInstantiationFailureException {
                    throw new RuntimeException(TestAction.class.getSimpleName() + "s are not instantiable");
                }
            };
            return ResultOrError.result(actionTemplate);
        }
    }
}
