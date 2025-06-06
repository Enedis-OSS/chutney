
/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.environment.domain.exception.AlreadyExistingTargetException;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.TargetNotFoundException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Environment {

    public final String name;
    public final String description;
    public final Set<Target> targets;
    public final Set<EnvironmentVariable> variables;

    private Environment(String name, String description, Set<Target> targets, Set<EnvironmentVariable> variables) {
        this.name = name;
        this.description = description;
        this.targets = targets;
        this.variables = variables;
    }

    public static EnvironmentBuilder builder() {
        return new EnvironmentBuilder();
    }

    Environment addTarget(Target target) {
        if (this.containsTarget(target)) {
            throw new AlreadyExistingTargetException(target.name, this.name);
        }

        return Environment.builder().from(this).addTarget(target).build();
    }

    private boolean containsTarget(Target target) {
        return targets.stream().anyMatch(t -> t.name.equalsIgnoreCase(target.name));
    }

    boolean containsVariable(String key) {
        return variables.stream().anyMatch(t -> t.key().equalsIgnoreCase(key));
    }

    Target getTarget(String targetName) {
        return targets.stream().filter(t -> t.name.equals(targetName)).findFirst().orElseThrow(() -> new TargetNotFoundException("Target [" + targetName + "] not found in environment [" + name + "]"));
    }

    Environment deleteTarget(String targetName) {
        Optional<Target> targetToRemove = targets.stream().filter(t -> t.name.equals(targetName)).findFirst();

        return targetToRemove.map(t -> {
            Set<Target> updatedTargets = new HashSet<>(targets);
            updatedTargets.remove(t);

            return Environment.builder().from(this).withTargets(updatedTargets).build();
        }).orElseThrow(() -> new TargetNotFoundException("Target [" + targetName + "] not found in environment [" + name + "]"));
    }

    Environment updateTarget(String targetName, Target targetToUpdate) {
        Optional<Target> previousTarget = targets.stream().filter(t -> t.name.equals(targetName)).findFirst();

        return previousTarget.map(t -> {
            if (previousTarget.get().equals(targetToUpdate)) {
                return this;
            }
            Set<Target> updatedTargets = new HashSet<>(targets);
            updatedTargets.remove(t);
            updatedTargets.add(targetToUpdate);

            return Environment.builder().from(this).withTargets(updatedTargets).build();
        }).orElseThrow(() -> new TargetNotFoundException("Target [" + targetName + "] not found in environment [" + name + "]"));
    }

    Environment addVariable(EnvironmentVariable variable) {
        if (this.containsVariable(variable.key())) {
            throw new VariableAlreadyExistingException("Variable [" + variable.key() + "] already exists in [" + this.name + "] environment");
        }
        return Environment.builder().from(this).addVariable(variable).build();
    }

    Environment updateVariable(String oldKey, EnvironmentVariable variable) {
        Optional<EnvironmentVariable> oldVariable = variables.stream().filter(t -> t.key().equalsIgnoreCase(oldKey)).findFirst();
        oldVariable.orElseThrow(() -> new EnvVariableNotFoundException("Variable [" + oldKey + "] not found in environment [" + name + "]"));
        if (!oldVariable.get().equals(variable)) {
            Set<EnvironmentVariable> updatedVariables = new HashSet<>(variables);
            updatedVariables.remove(oldVariable.get());
            updatedVariables.add(variable);
            return Environment.builder()
                .from(this)
                .withVariables(updatedVariables).build();
        }
        return this;
    }

    Environment deleteVariable(String key) {
        Optional<EnvironmentVariable> variable = variables.stream().filter(t -> t.key().equalsIgnoreCase(key)).findFirst();
        if (variable.isEmpty()) {
            return this;
        }
        Set<EnvironmentVariable> updatedVariables = new HashSet<>(variables);
        updatedVariables.remove(variable.get());
        return Environment.builder()
            .from(this)
            .withVariables(updatedVariables)
            .build();
    }

    public static class EnvironmentBuilder {

        private String name;
        private String description;
        private Set<Target> targets = new HashSet<>();
        private Set<EnvironmentVariable> variables = new HashSet<>();

        private EnvironmentBuilder() {
        }

        public Environment build() {
            return new Environment(ofNullable(name).orElse(""),
                ofNullable(description).orElse(""),
                ofNullable(targets).map(Collections::unmodifiableSet).orElse(emptySet()),
                ofNullable(variables).map(Collections::unmodifiableSet).orElse(emptySet()));
        }

        public EnvironmentBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public EnvironmentBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public EnvironmentBuilder withTargets(Set<Target> targetSet) {
            this.targets = new HashSet<>(targetSet);
            return this;
        }

        public EnvironmentBuilder withVariables(Set<EnvironmentVariable> variables) {
            this.variables = new HashSet<>(variables);
            return this;
        }


        public EnvironmentBuilder addTarget(Target target) {
            this.targets.add(target);
            return this;
        }

        public EnvironmentBuilder addVariable(EnvironmentVariable variable) {
            this.variables.add(variable);
            return this;
        }

        public EnvironmentBuilder from(Environment environment) {
            this.name = environment.name;
            this.description = environment.description;
            this.targets = new HashSet<>(environment.targets);
            this.variables = new HashSet<>(environment.variables);
            return this;
        }

    }

    @Override
    public String toString() {
        return "Environment{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", targets=" + targets +
            ", variables=" + variables +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Environment that = (Environment) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(targets, that.targets) && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, targets, variables);
    }

}
