/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.environment;

import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.domain.EnvironmentService;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.CannotDeleteEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EmbeddedEnvironmentApi implements EnvironmentApi {

    private final EnvironmentService environmentService;

    public EmbeddedEnvironmentApi(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public Set<EnvironmentDto> listEnvironments() {
        return environmentService.listEnvironments().stream()
            .map(EnvironmentDto::from)
            .sorted(Comparator.comparing(e -> e.name))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<String> listEnvironmentsNames() {
        return environmentService.listEnvironmentsNames();
    }


    @Override
    public String defaultEnvironmentName() throws EnvironmentNotFoundException {
        return environmentService.defaultEnvironmentName();
    }

    @Override
    public EnvironmentDto getEnvironment(String environmentName) throws EnvironmentNotFoundException {
        return EnvironmentDto.from(environmentService.getEnvironment(environmentName));
    }

    @Override
    public EnvironmentDto createEnvironment(EnvironmentDto environmentMetadataDto, boolean force) throws InvalidEnvironmentNameException, AlreadyExistingEnvironmentException {
        return EnvironmentDto.from(environmentService.createEnvironment(environmentMetadataDto.toEnvironment(), force));
    }

    @Override
    public EnvironmentDto importEnvironment(EnvironmentDto environmentDto) throws UnsupportedOperationException {
        environmentService.createEnvironment(environmentDto.toEnvironment());
        return environmentDto;
    }

    @Override
    public void deleteEnvironment(String environmentName) throws EnvironmentNotFoundException, CannotDeleteEnvironmentException {
        environmentService.deleteEnvironment(environmentName);
    }

    @Override
    public void updateEnvironment(String environmentName, EnvironmentDto environmentMetadataDto) throws InvalidEnvironmentNameException, EnvironmentNotFoundException {
        environmentService.updateEnvironment(environmentName, environmentMetadataDto.toEnvironment());
    }


}
