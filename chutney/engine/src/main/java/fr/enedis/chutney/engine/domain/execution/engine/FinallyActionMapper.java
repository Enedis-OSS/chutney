/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine;

import static java.util.Collections.emptyMap;

import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.StepDefinition;
import fr.enedis.chutney.engine.domain.execution.strategies.StepStrategyDefinition;
import fr.enedis.chutney.engine.domain.execution.strategies.StrategyProperties;

class FinallyActionMapper {

    StepDefinition toStepDefinition(FinallyAction finallyAction) {
        return new StepDefinition(
            finallyAction.name(),
            finallyAction.target()
                .orElse(TargetImpl.NONE),
            finallyAction.type(),
            finallyAction.strategyType()
                .map(st -> new StepStrategyDefinition(st, new StrategyProperties(finallyAction.strategyProperties().orElse(emptyMap()))))
                .orElse(null),
            finallyAction.inputs(),
            null,
            null,
            finallyAction.validations()
        );
    }
}
