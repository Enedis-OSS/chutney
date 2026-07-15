/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.mapper;

import static org.hjson.JsonValue.readHjson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.enedis.chutney.execution.domain.GwtScenarioMarshaller;
import fr.enedis.chutney.scenario.api.raw.dto.GwtScenarioDto;
import fr.enedis.chutney.scenario.api.raw.dto.GwtStepDto;
import fr.enedis.chutney.scenario.api.raw.dto.GwtStepImplementationDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtScenarioDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtStepDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtStepImplementationDto;
import fr.enedis.chutney.scenario.api.raw.dto.StrategyDto;
import fr.enedis.chutney.scenario.domain.gwt.GwtScenario;
import fr.enedis.chutney.scenario.domain.gwt.GwtStep;
import fr.enedis.chutney.scenario.domain.gwt.GwtStepImplementation;
import fr.enedis.chutney.scenario.domain.gwt.Strategy;
import fr.enedis.chutney.server.core.domain.execution.ScenarioConversionException;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotParsableException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hjson.Stringify;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GwtScenarioMapper implements GwtScenarioMarshaller {

    // TODO - Refactor mappers scattered everywhere :)
    public static ObjectMapper mapper = JsonMapper.builder()
        .findAndAddModules()
        .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY))
        .changeDefaultVisibility(v -> v
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE))
        .addMixIn(GwtScenario.class, GwtScenarioMixin.class)
        .addMixIn(GwtStep.class, GwtStepMixin.class)
        .addMixIn(GwtStep.GwtStepBuilder.class, GwtStepBuilderMixin.class)
        .addMixIn(GwtStepImplementation.class, GwtStepImplementationMixin.class)
        .addMixIn(Strategy.class, StrategyMixin.class)
        .build();

    @JsonDeserialize(builder = GwtScenario.GwtScenarioBuilder.class)
    private static class GwtScenarioMixin {
    }

    @JsonDeserialize(builder = GwtStep.GwtStepBuilder.class)
    private static class GwtStepMixin {
    }

    private static class GwtStepBuilderMixin {
        @JsonProperty("x-$ref")
        String xRef;
    }

    private static class GwtStepImplementationMixin {

        @JsonProperty("x-$ref")
        String xRef;

        @JsonCreator
        public GwtStepImplementationMixin(String type,
                                          String target,
                                          @JsonInclude(JsonInclude.Include.ALWAYS) Map<String, Object> inputs, // Do not remove JsonInclude
                                          Map<String, Object> outputs,
                                          Map<String, Object> validations,
                                          String xRef
        ) {
        }
    }

    private static class StrategyMixin {
        @JsonCreator
        public StrategyMixin(String type,
                             Map<String, Object> parameters) {
        }
    }

    // DTO -> Scenario
    public static GwtScenario fromDto(String title, String desc, GwtScenarioDto dto) {
        return GwtScenario.builder()
            .withTitle(title)
            .withDescription(desc)
            .withGivens(dto.givens().stream().map(GwtScenarioMapper::fromDto).collect(Collectors.toList()))
            .withWhen(GwtScenarioMapper.fromDto(dto.when()))
            .withThens(dto.thens().stream().map(GwtScenarioMapper::fromDto).collect(Collectors.toList()))
            .build();
    }

    // DTO -> Step
    private static GwtStep fromDto(GwtStepDto dto) {
        GwtStep.GwtStepBuilder builder = GwtStep.builder();

        dto.sentence().ifPresent(builder::withDescription);
        dto.xRef().ifPresent(builder::withXRef);
        builder.withSubSteps(dto.subSteps().stream().map(GwtScenarioMapper::fromDto).collect(Collectors.toList()));
        dto.implementation().ifPresent(i -> builder.withImplementation(fromDto(i)));
        dto.strategy().ifPresent(s -> builder.withStrategy(new Strategy(s.getType(), s.getParameters())));

        return builder.build();
    }

    // DTO -> Implementation
    private static GwtStepImplementation fromDto(GwtStepImplementationDto dto) {
        if (dto.task().isEmpty()) {
            return new GwtStepImplementation(dto.type(), dto.target(), dto.inputs(), dto.outputs(), dto.validations(), dto.xRef());
        } else {
            try {
                return mapper.readValue(readHjson(dto.task()).toString(), GwtStepImplementation.class);
            } catch (JacksonException e) {
                throw new ScenarioConversionException(e);
            }
        }
    }

    public static GwtScenarioDto toDto(GwtScenario scenario) {
        return ImmutableGwtScenarioDto.builder()
            .givens(toDto(scenario.givens))
            .when(toDto(scenario.when))
            .thens(toDto(scenario.thens))
            .build();
    }

    private static List<GwtStepDto> toDto(List<GwtStep> givens) {
        return givens.stream().map(GwtScenarioMapper::toDto).collect(Collectors.toList());
    }

    private static GwtStepDto toDto(GwtStep step) {
        ImmutableGwtStepDto.Builder builder = ImmutableGwtStepDto.builder();
        builder.sentence(step.description);
        step.implementation.ifPresent(i -> builder.implementation(toDto(i)));
        step.strategy.ifPresent(s -> builder.strategy(toDto(s)));
        step.xRef.ifPresent(builder::xRef);
        builder.subSteps(toDto(step.subSteps));
        return builder.build();
    }

    private static GwtStepImplementationDto toDto(GwtStepImplementation implementation) {
        try {
            return ImmutableGwtStepImplementationDto.builder()
                .task(readHjson(mapper.writeValueAsString(implementation)).toString(Stringify.HJSON))
                .type(implementation.type)
                .target(implementation.target)
                .xRef(implementation.xRef)
                .inputs(implementation.inputs)
                .outputs(implementation.outputs)
                .build();
        } catch (Exception e) {
            throw new ScenarioNotParsableException("Cannot deserialize action implementation", e);
        }
    }

    private static StrategyDto toDto(Strategy strategy) {
        return new StrategyDto(strategy.type, strategy.parameters);
    }

    @Override
    public String serialize(GwtScenario scenario) {
        try {
            return mapper.writeValueAsString(scenario);
        } catch (JacksonException e) {
            throw new ScenarioNotParsableException("Cannot serialize scenario: " + e.getMessage(), e);
        }
    }

    @Override
    public GwtScenario deserialize(String title, String description, String jsonScenario) {
        try {
            return mapper.readValue(jsonScenario, GwtScenario.class);
        } catch (JacksonException e) {
            throw new ScenarioNotParsableException("Cannot deserialize scenario: ", e);
        }
    }

}
