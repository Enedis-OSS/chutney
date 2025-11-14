/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import fr.enedis.chutney.environment.api.environment.EnvironmentController;
import fr.enedis.chutney.environment.domain.Environment;
import fr.enedis.chutney.environment.domain.EnvironmentRepository;
import fr.enedis.chutney.environment.domain.EnvironmentService;
import fr.enedis.chutney.environment.domain.EnvironmentVariable;
import fr.enedis.chutney.environment.domain.Target;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class TestHelper {

    protected final String environmentBasePath = EnvironmentController.BASE_URL;

    protected final Map<String, Environment> registeredEnvironments = new LinkedHashMap<>();
    protected final EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);

    protected final EnvironmentService environmentService = new EnvironmentService(environmentRepository);
    protected final EnvironmentApi environmentApi = new EmbeddedEnvironmentApi(environmentService);


    public void addAvailableEnvironment(String envName) {
        addAvailableEnvironment(envName, emptyList(), emptyList());
    }

    public void addAvailableEnvironment(String envName, List<String> targetsNames) {
        addAvailableEnvironment(envName, targetsNames, emptyList());
    }

    public void addAvailableEnvironment(String envName, List<String> targetsNames, List<String> variablesNames) {
        Set<Target> targets = targetsNames.stream()
            .map(targetName -> Target.builder()
                .withName(targetName)
                .withEnvironment(envName)
                .withUrl("http://" + targetName.replace(' ', '_') + ":43")
                .build())
            .collect(toCollection(LinkedHashSet::new));

        Set<EnvironmentVariable> variables = variablesNames.stream()
            .map(name -> new EnvironmentVariable(name, name, envName)).collect(toCollection(LinkedHashSet::new));


        registeredEnvironments.put(
            envName,
            Environment.builder()
                .withName(envName)
                .withDescription(envName + " description")
                .withTargets(targets)
                .withVariables(variables)
                .build()
        );

        when(environmentRepository.findByName(eq(envName)))
            .thenAnswer(iom -> {
                    String envNameParam = iom.getArgument(0);
                    if (!registeredEnvironments.containsKey(envNameParam)) {
                        throw new EnvironmentNotFoundException(List.of("test env not found"));
                    }
                    return registeredEnvironments.get(envNameParam);
                }
            );

        when(environmentRepository.listNames())
            .thenReturn(new ArrayList<>(registeredEnvironments.keySet()));
    }
}
