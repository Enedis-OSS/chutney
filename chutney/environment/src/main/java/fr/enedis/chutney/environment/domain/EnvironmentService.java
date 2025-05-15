/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain;

import static java.util.Collections.emptyList;

import fr.enedis.chutney.environment.domain.exception.AlreadyExistingEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingTargetException;
import fr.enedis.chutney.environment.domain.exception.CannotDeleteEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import fr.enedis.chutney.environment.domain.exception.NoEnvironmentFoundException;
import fr.enedis.chutney.environment.domain.exception.SingleEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.TargetNotFoundException;
import fr.enedis.chutney.environment.domain.exception.UnresolvedEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import fr.enedis.chutney.server.core.domain.environment.UpdateEnvironmentHandler;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentService {

    private static final String NAME_VALIDATION_REGEX = "[a-zA-Z0-9_\\-]{3,20}";
    private static final Pattern NAME_VALIDATION_PATTERN = Pattern.compile(NAME_VALIDATION_REGEX);

    private final Logger logger = LoggerFactory.getLogger(EnvironmentService.class);
    private final EnvironmentRepository environmentRepository;
    private final List<UpdateEnvironmentHandler> updateEnvironmentHandlers;

    public EnvironmentService(EnvironmentRepository environmentRepository, List<UpdateEnvironmentHandler> updateEnvironmentHandlers) {
        this.environmentRepository = environmentRepository;
        this.updateEnvironmentHandlers = Optional.ofNullable(updateEnvironmentHandlers).orElse(emptyList());
    }

    public EnvironmentService(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
        this.updateEnvironmentHandlers = emptyList();
    }

    public Set<String> listEnvironmentsNames() {
        return new LinkedHashSet<>(environmentRepository.listNames());
    }

    public Set<Environment> listEnvironments() {
        return environmentRepository
            .listNames()
            .stream()
            .map(environmentRepository::findByName)
            .collect(Collectors.toSet());
    }

    public Environment createEnvironment(Environment environment) throws InvalidEnvironmentNameException, AlreadyExistingEnvironmentException {
        return createEnvironment(environment, false);
    }

    public Environment createEnvironment(Environment environment, boolean force) throws InvalidEnvironmentNameException, AlreadyExistingEnvironmentException {
        if (!force && envAlreadyExist(environment)) {
            throw new AlreadyExistingEnvironmentException(environment.name);
        }
        createOrUpdate(environment);
        return environment;
    }

    public Environment getEnvironment(String environmentName) throws EnvironmentNotFoundException {
        return environmentRepository.findByName(environmentName);
    }

    public void deleteEnvironment(String environmentName) throws EnvironmentNotFoundException, CannotDeleteEnvironmentException {
        List<String> environmentNames = environmentRepository.listNames();
        if (environmentNames.stream().noneMatch(env -> env.equals(environmentName))) {
            logger.error("Environment not found for name {}", environmentName);
            throw new EnvironmentNotFoundException(environmentNames);
        }
        if (environmentNames.size() == 1) {
            logger.error("Cannot delete environment with name {} : cannot delete the last env", environmentName);
            throw new SingleEnvironmentException(environmentName);
        }
        environmentRepository.delete(environmentName);
        updateEnvironmentHandlers.forEach(renameEnvironmentHandler -> renameEnvironmentHandler.deleteEnvironment(environmentName));
    }

    public void updateEnvironment(String environmentName, Environment newVersion) throws InvalidEnvironmentNameException, EnvironmentNotFoundException {
        Environment previousEnvironment = environmentRepository.findByName(environmentName);
        Environment newEnvironment = Environment.builder()
            .from(previousEnvironment)
            .withName(newVersion.name)
            .withDescription(newVersion.description)
            .build();
        createOrUpdate(newEnvironment);
        if (!newEnvironment.name.equals(environmentName)) {
            environmentRepository.delete(environmentName);
            updateEnvironmentHandlers.forEach(renameEnvironmentHandler -> renameEnvironmentHandler.renameEnvironment(environmentName, newEnvironment.name));
        }
    }

    public String defaultEnvironmentName() throws EnvironmentNotFoundException {
        List<String> envs = environmentRepository.listNames();
        if (envs.size() > 1) {
            throw new UnresolvedEnvironmentException();
        }
        if (envs.isEmpty()) {
            throw new NoEnvironmentFoundException();
        }

        return envs.getFirst();
    }


    public List<Target> listTargets(TargetFilter filters) {
        Set<Target> targets;
        if (filters != null && StringUtils.isNotBlank(filters.environment())) {
            targets = environmentRepository.findByName(filters.environment()).targets;
        } else {
            targets = listEnvironments()
                .stream()
                .flatMap(environment -> environment.targets.stream()).collect(Collectors.toSet());
        }
        return targets
            .stream()
            .filter(target -> match(target, filters))
            .collect(Collectors.toList());
    }


    public Set<String> listTargetsNames() {
        return listEnvironments().stream()
            .flatMap(environment -> environment.targets.stream().map(target -> target.name))
            .collect(Collectors.toSet());

    }

    public Target getTarget(String environmentName, String targetName) {
        Environment environment = environmentRepository.findByName(environmentName);
        return environment.getTarget(targetName);
    }

    public void addTarget(Target target) throws EnvironmentNotFoundException, AlreadyExistingTargetException {
        Environment environment = environmentRepository.findByName(target.environment);
        Environment newEnvironment = environment.addTarget(target);
        createOrUpdate(newEnvironment);
    }

    public void deleteTarget(String environmentName, String targetName) throws EnvironmentNotFoundException, TargetNotFoundException {
        Environment environment = environmentRepository.findByName(environmentName);
        Environment newEnvironment = environment.deleteTarget(targetName);
        createOrUpdate(newEnvironment);
    }

    public void deleteTarget(String targetName) throws EnvironmentNotFoundException, TargetNotFoundException {
        environmentRepository.getEnvironments()
            .stream()
            .filter(env -> env.targets.stream().map(target -> target.name).toList().contains(targetName))
            .forEach(env -> {
                Environment newEnvironment = env.deleteTarget(targetName);
                createOrUpdate(newEnvironment);
            });
    }

    public void updateTarget(String previousTargetName, Target targetToUpdate) throws EnvironmentNotFoundException, TargetNotFoundException {
        Environment environment = environmentRepository.findByName(targetToUpdate.environment);
        Environment newEnvironment = environment.updateTarget(previousTargetName, targetToUpdate);
        createOrUpdate(newEnvironment);
        logger.debug("Updated target " + previousTargetName + " as " + targetToUpdate.name);
    }

    public void addVariable(List<EnvironmentVariable> values) throws EnvironmentNotFoundException, VariableAlreadyExistingException {
        values.forEach(variable -> {
            Environment environment = environmentRepository.findByName(variable.env());
            this.addVariable(variable, environment);
        });

    }

    public void createOrUpdateVariable(String existingKey, List<EnvironmentVariable> values) throws EnvironmentNotFoundException, EnvVariableNotFoundException {
        values.forEach(variable -> {
            Environment environment = environmentRepository.findByName(variable.env());
            if (!environment.containsVariable(existingKey)) {
                this.addVariable(variable, environment);
                return;
            }
            Environment updated = environment.updateVariable(existingKey, variable);
            if (!environment.equals(updated)) {
                createOrUpdate(updated);
                logger.debug("Updated variable " + existingKey + " as " + values.getFirst().key());
            }
        });
    }

    public void deleteVariable(String key) {
        this.deleteVariable(key, environmentRepository.listNames());
    }

    public void deleteVariable(String key, List<String> envs) {
        List<Environment> environments = environmentRepository.findByNames(envs)
            .stream()
            .filter(env -> env.containsVariable(key)).toList();

        if (!envs.isEmpty() && environments.isEmpty()) {
            throw new EnvVariableNotFoundException(key);
        }
        environments
            .forEach(env -> {
                Environment updated = env.deleteVariable(key);
                createOrUpdate(updated);
            });
        logger.debug("Deleted variable: {}", key);
    }

    private void addVariable(EnvironmentVariable variable, Environment env) throws EnvironmentNotFoundException, VariableAlreadyExistingException {
        Environment updated = env.addVariable(variable);
        createOrUpdate(updated);
        logger.debug("Variable " + variable.key() + " added to environment " + env);

    }

    protected void createOrUpdate(Environment environment) {
        if (!NAME_VALIDATION_PATTERN.matcher(environment.name).matches()) {
            throw new InvalidEnvironmentNameException();
        }
        environmentRepository.save(environment);
    }

    private boolean envAlreadyExist(Environment environment) {
        return environmentRepository.listNames().stream().map(String::toUpperCase)
            .toList().contains(environment.name.toUpperCase());
    }

    private boolean match(Target target, TargetFilter filters) {
        if (filters == null) {
            return true;
        }

        boolean matchName = true;
        if (StringUtils.isNotBlank(filters.name())) {
            matchName = filters.name().equals(target.name);
        }

        return matchName;
    }
}
