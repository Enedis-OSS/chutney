/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.api;

import fr.enedis.chutney.action.api.ActionDto.InputsDto;
import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.parameter.Parameter;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Target;
import java.util.List;

public class ActionTemplateMapper {

    private ActionTemplateMapper() {
    }

    public static ActionDto toDto(ActionTemplate actionTemplate) {
        return new ActionDto(actionTemplate.identifier(),
            hasTarget(actionTemplate),
            toInputsDto(actionTemplate)
        );
    }

    private static boolean hasTarget(ActionTemplate actionTemplate) {
        return actionTemplate.parameters().stream().anyMatch(p -> p.rawType().equals(Target.class));
    }

    private static List<InputsDto> toInputsDto(ActionTemplate actionTemplate) {
       return actionTemplate.parameters().stream()
            .filter(parameter -> parameter.annotations().optional(Input.class).isPresent())
            .map(ActionTemplateMapper::simpleParameterToInputsDto)
            .toList();
    }

    private static InputsDto simpleParameterToInputsDto(Parameter parameter) {
        return new InputsDto(parameter.annotations().get(Input.class).value(), parameter.rawType());
    }
}
