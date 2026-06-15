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
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtScenarioDto.class)
@Value.Style(jdkOnly = true)
public interface GwtScenarioDto {

    @JsonProperty("givens")
    List<GwtStepDto> givens();

    @JsonProperty("when")
    GwtStepDto when();

    @JsonProperty("thens")
    List<GwtStepDto> thens();

    @JsonCreator
    static GwtScenarioDto of(
        @JsonProperty("givens") @Nullable List<GwtStepDto> givens,
        @JsonProperty("when") GwtStepDto when,
        @JsonProperty("thens") @Nullable List<GwtStepDto> thens
    ) {
        ImmutableGwtScenarioDto.Builder builder = ImmutableGwtScenarioDto.builder().when(when);
        if (givens != null) builder.givens(givens);
        if (thens != null) builder.thens(thens);
        return builder.build();
    }
}
