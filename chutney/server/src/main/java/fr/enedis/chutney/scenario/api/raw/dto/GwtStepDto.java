/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtStepDto.class)
@Value.Style(jdkOnly = true)
public interface GwtStepDto {

    @JsonProperty("sentence")
    Optional<String> sentence();

    @JsonProperty("subSteps")
    List<GwtStepDto> subSteps();

    @JsonProperty("implementation")
    Optional<GwtStepImplementationDto> implementation();

    @JsonProperty("strategy")
    Optional<StrategyDto> strategy();

    @JsonProperty("x-$ref") Optional<String> xRef();

    @JsonCreator
    static GwtStepDto of(
        @JsonProperty("sentence") @Nullable String sentence,
        @JsonProperty("subSteps") @Nullable List<GwtStepDto> subSteps,
        @JsonProperty("implementation") @Nullable GwtStepImplementationDto implementation,
        @JsonProperty("strategy") @Nullable StrategyDto strategy,
        @JsonProperty("x-$ref") @Nullable String xRef
    ) {
        ImmutableGwtStepDto.Builder builder = ImmutableGwtStepDto.builder();
        if (sentence != null) builder.sentence(sentence);
        if (subSteps != null) builder.subSteps(subSteps);
        if (implementation != null) builder.implementation(implementation);
        if (strategy != null) builder.strategy(strategy);
        if (xRef != null) builder.xRef(xRef);
        return builder.build();
    }
}
