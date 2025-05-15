/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.target;

import fr.enedis.chutney.environment.api.target.dto.TargetDto;
import fr.enedis.chutney.environment.domain.TargetFilter;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingTargetException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.TargetNotFoundException;
import java.util.List;
import java.util.Set;

public interface TargetApi {
    List<TargetDto> listTargets(TargetFilter filter) throws EnvironmentNotFoundException;

    Set<String> listTargetsNames() throws EnvironmentNotFoundException;

    TargetDto getTarget(String environmentName, String targetName) throws EnvironmentNotFoundException, TargetNotFoundException;

    void addTarget(TargetDto targetMetadataDto) throws EnvironmentNotFoundException, AlreadyExistingTargetException;

    TargetDto importTarget(String environmentName, TargetDto targetDto);

    void updateTarget(String targetName, TargetDto targetMetadataDto) throws EnvironmentNotFoundException, TargetNotFoundException;

    void deleteTarget(String environmentName, String targetName) throws EnvironmentNotFoundException, TargetNotFoundException;

    void deleteTarget(String targetName) throws EnvironmentNotFoundException, TargetNotFoundException;

}
