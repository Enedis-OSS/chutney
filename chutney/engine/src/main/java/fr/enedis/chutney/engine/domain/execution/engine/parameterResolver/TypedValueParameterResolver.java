/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.parameterResolver;

import fr.enedis.chutney.action.domain.parameter.Parameter;
import fr.enedis.chutney.action.domain.parameter.ParameterResolver;

public class TypedValueParameterResolver<T> implements ParameterResolver {

    private final Class<? extends T> matchingType;
    private final T value;

    public TypedValueParameterResolver(Class<? extends T> matchingType, T value) {
        this.matchingType = matchingType;
        this.value = value;
    }

    @Override
    public boolean canResolve(Parameter parameter) {
        return matchingType.equals(parameter.rawType());
    }

    @Override
    public Object resolve(Parameter parameter) {
        return value;
    }
}
