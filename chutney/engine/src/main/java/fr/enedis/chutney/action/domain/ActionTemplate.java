/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.domain;

import fr.enedis.chutney.action.domain.parameter.Parameter;
import fr.enedis.chutney.action.domain.parameter.ParameterResolver;
import fr.enedis.chutney.action.spi.Action;
import java.util.List;
import java.util.Set;

/**
 * Template for creating {@link Action} instances.<br>
 * This object exposes all parameters (names and types) needed by a {@link Action}
 */
public interface ActionTemplate {

    /**
     * @return an identifier to link action description in a scenario and its implementation
     */
    String identifier();

    /**
     * @return the class parsed into the current {@link ActionTemplate}. May not be a {@link Action} if adaptation is made to comply to the current SPI.
     */
    Class<?> implementationClass();

    /**
     * @return {@link Parameter}s needed to create an instance of {@link Action}
     */
    Set<Parameter> parameters();

    Action create(List<ParameterResolver> parameterResolvers) throws UnresolvableActionParameterException, ActionInstantiationFailureException;

    default <T> T resolveParameter(List<ParameterResolver> parameterResolvers, Parameter parameter) {
        return (T) parameterResolvers
            .stream()
            .filter(pr -> pr.canResolve(parameter))
            .findFirst()
            .orElseThrow(() -> new UnresolvableActionParameterException(identifier(), parameter))
            .resolve(parameter);
    }
}
