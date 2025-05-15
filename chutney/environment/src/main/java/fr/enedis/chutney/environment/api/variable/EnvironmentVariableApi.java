/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.variable;


import fr.enedis.chutney.environment.api.variable.dto.EnvironmentVariableDto;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import java.util.List;

public interface EnvironmentVariableApi {

    void addVariable(List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, VariableAlreadyExistingException;

    void updateVariable(String key, List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, EnvVariableNotFoundException;

    void deleteVariable(String key) throws  EnvVariableNotFoundException;

}
