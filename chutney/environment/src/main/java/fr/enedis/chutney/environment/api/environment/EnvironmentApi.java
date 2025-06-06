/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.environment;

import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.CannotDeleteEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import java.util.Set;

public interface EnvironmentApi {
    Set<EnvironmentDto> listEnvironments();

    Set<String> listEnvironmentsNames();

    String defaultEnvironmentName();

    EnvironmentDto getEnvironment(String environmentName) throws EnvironmentNotFoundException;

    default EnvironmentDto createEnvironment(EnvironmentDto environmentMetadataDto) throws InvalidEnvironmentNameException, AlreadyExistingEnvironmentException {
        return createEnvironment(environmentMetadataDto, false);
    }

    EnvironmentDto createEnvironment(EnvironmentDto environmentMetadataDto, boolean force) throws InvalidEnvironmentNameException, AlreadyExistingEnvironmentException;

    EnvironmentDto importEnvironment(EnvironmentDto environmentDto);

    void updateEnvironment(String environmentName, EnvironmentDto environmentMetadataDto) throws InvalidEnvironmentNameException, EnvironmentNotFoundException;

    void deleteEnvironment(String environmentName) throws EnvironmentNotFoundException, CannotDeleteEnvironmentException;


}
