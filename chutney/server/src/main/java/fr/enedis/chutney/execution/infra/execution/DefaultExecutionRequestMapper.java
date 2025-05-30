/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.execution;

import static fr.enedis.chutney.environment.api.target.dto.NoTargetDto.NO_TARGET_DTO;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import fr.enedis.chutney.agent.domain.explore.CurrentNetworkDescription;
import fr.enedis.chutney.agent.domain.network.Agent;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.engine.api.execution.EnvironmentDto;
import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto;
import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto.StepDefinitionRequestDto;
import fr.enedis.chutney.engine.api.execution.TargetExecutionDto;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import fr.enedis.chutney.environment.api.target.EmbeddedTargetApi;
import fr.enedis.chutney.environment.api.target.TargetApi;
import fr.enedis.chutney.environment.api.target.dto.TargetDto;
import fr.enedis.chutney.environment.api.variable.dto.EnvironmentVariableDto;
import fr.enedis.chutney.scenario.domain.gwt.GwtStep;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.scenario.domain.gwt.Strategy;
import fr.enedis.chutney.server.core.domain.execution.ExecutionRequest;
import fr.enedis.chutney.server.core.domain.execution.ScenarioConversionException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultExecutionRequestMapper implements ExecutionRequestMapper {

    private final TargetApi targetApi;
    private final EnvironmentApi environmentApi;
    private final CurrentNetworkDescription currentNetworkDescription;

    public DefaultExecutionRequestMapper(EmbeddedTargetApi targetApi, EmbeddedEnvironmentApi environmentApi, CurrentNetworkDescription currentNetworkDescription) {
        this.targetApi = targetApi;
        this.environmentApi = environmentApi;
        this.currentNetworkDescription = currentNetworkDescription;
    }

    @Override
    public ExecutionRequestDto toDto(ExecutionRequest executionRequest) {
        final StepDefinitionRequestDto stepDefinitionRequestDto = convertToStepDef(executionRequest);
        return new ExecutionRequestDto(stepDefinitionRequestDto, getEnvironment(executionRequest.environment), DatasetMapper.toDto(executionRequest.dataset));
    }

    private EnvironmentDto getEnvironment(String env) {
        Map<String, String> variables = environmentApi.getEnvironment(env).variables
            .stream()
            .collect(Collectors.toMap(EnvironmentVariableDto::key, EnvironmentVariableDto::value));
        return new EnvironmentDto(env, variables);
    }

    private StepDefinitionRequestDto convertToStepDef(ExecutionRequest executionRequest) { // TODO - shameless green - might be refactored later
        if (executionRequest.testCase instanceof GwtTestCase) {
            return convertGwt(executionRequest);
        }

        throw new ScenarioConversionException(executionRequest.testCase.metadata().id(),
            "Cannot create an executable StepDefinition from a " + executionRequest.testCase.getClass().getCanonicalName());
    }

    private StepDefinitionRequestDto convertGwt(ExecutionRequest executionRequest) {
        GwtTestCase gwtTestCase = (GwtTestCase) executionRequest.testCase;
        return new StepDefinitionRequestDto(
            gwtTestCase.metadata.title,
            null,
            null,
            null,
            emptyMap(),
            convert(gwtTestCase.scenario.steps(), executionRequest.environment),
            emptyMap(),
            emptyMap()
        );
    }

    private List<StepDefinitionRequestDto> convert(List<GwtStep> steps, String env) {
        return steps.stream()
            .map(s -> convert(s, env))
            .collect(toList());
    }

    private StepDefinitionRequestDto convert(GwtStep step, String env) {
        return new StepDefinitionRequestDto(
            step.description,
            step.implementation.map(i -> toExecutionTargetDto(getTargetForExecution(env, i.target), env)).orElse(toExecutionTargetDto(NO_TARGET_DTO, env)),
            step.strategy.map(this::mapStrategy).orElse(null),
            step.implementation.map(i -> i.type).orElse(""),
            step.implementation.map(i -> i.inputs).orElse(emptyMap()),
            convert(step.subSteps, env),
            step.implementation.map(i -> i.outputs).orElse(emptyMap()),
            step.implementation.map(i -> i.validations).orElse(emptyMap())
        );
    }

    private ExecutionRequestDto.StepStrategyDefinitionRequestDto mapStrategy(Strategy strategy) {
        return new ExecutionRequestDto.StepStrategyDefinitionRequestDto(
            strategy.type,
            strategy.parameters
        );
    }

    private TargetExecutionDto toExecutionTargetDto(TargetDto targetDto, String env) {
        if (targetDto == null || NO_TARGET_DTO.equals(targetDto)) {
            targetDto = NO_TARGET_DTO;
        }
        return new TargetExecutionDto(
            targetDto.name,
            targetDto.url,
            targetDto.propertiesToMap(),
            getAgents(targetDto, env)
        );
    }

    private TargetDto getTargetForExecution(String environmentName, String targetName) {
        if (isBlank(targetName)) {
            return NO_TARGET_DTO;
        }
        return targetApi.getTarget(environmentName, targetName);
    }

    private List<NamedHostAndPort> getAgents(TargetDto targetDto, String env) {
        List<NamedHostAndPort> nhaps = emptyList();
        Optional<NetworkDescription> networkDescription = currentNetworkDescription.findCurrent();
        if (networkDescription.isPresent() && networkDescription.get().localAgent().isPresent()) {
            final Agent localAgent = networkDescription.get().localAgent().get();
            List<Agent> agents = localAgent.findFellowAgentForReaching(targetDto.name, env);
            nhaps = agents.stream().map(a -> a.agentInfo).collect(toList());
        }
        return nhaps;
    }
}
