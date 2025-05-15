/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.variable;

import fr.enedis.chutney.environment.api.variable.dto.EnvironmentVariableDto;
import fr.enedis.chutney.environment.api.variable.dto.EnvironmentVariableDtoMapper;
import fr.enedis.chutney.environment.domain.EnvironmentService;
import fr.enedis.chutney.environment.domain.EnvironmentVariable;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class EmbeddedVariableApi implements EnvironmentVariableApi {

    private final EnvironmentService environmentService;

    private final EnvironmentVariableDtoMapper variableDtoMapper;

    public EmbeddedVariableApi(EnvironmentService environmentService) {
        this.environmentService = environmentService;
        this.variableDtoMapper = EnvironmentVariableDtoMapper.INSTANCE;
    }


    @Override
    public void addVariable(List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, VariableAlreadyExistingException {
        environmentService.addVariable(variableDtoMapper.toDomains(values));
    }

    @Override
    public void updateVariable(String key, List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, EnvVariableNotFoundException {
        List<EnvironmentVariable> variables = variableDtoMapper.toDomains(values);
        Map<Boolean, List<EnvironmentVariable>> partitionedVariables = variables.stream().collect(Collectors.partitioningBy(item -> StringUtils.isNotBlank(item.value())));
        List<EnvironmentVariable> toBeCreatedOrUpdated = partitionedVariables.get(true);
        List<EnvironmentVariable> toBeDeleted = partitionedVariables.get(false);
        environmentService.createOrUpdateVariable(key, toBeCreatedOrUpdated);
        try {
            environmentService.deleteVariable(key, toBeDeleted.stream().map(EnvironmentVariable::env).toList());
        } catch (EnvVariableNotFoundException ignored) {}
    }

    @Override
    public void deleteVariable(String key) throws EnvVariableNotFoundException {
        environmentService.deleteVariable(key);
    }
}
