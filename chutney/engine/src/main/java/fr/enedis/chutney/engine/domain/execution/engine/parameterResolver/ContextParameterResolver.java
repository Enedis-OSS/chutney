/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.parameterResolver;

import fr.enedis.chutney.action.domain.parameter.Parameter;
import fr.enedis.chutney.action.domain.parameter.ParameterResolver;
import java.util.Map;

public class ContextParameterResolver implements ParameterResolver {

    private final Map<String, Object> inputs;

    public ContextParameterResolver(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    @Override
    public boolean canResolve(Parameter parameter) {
        return parameter.annotations().isEmpty() && parameter.rawType().equals(Map.class);
    }

    @Override
    public Object resolve(Parameter parameter) {
        return inputs;
    }

}
