/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtStepDto.class)
@JsonDeserialize(as = ImmutableGwtStepDto.class)
@Value.Style(jdkOnly = true)
public interface GwtStepDto {

    Optional<String> sentence();

    List<GwtStepDto> subSteps();

    Optional<GwtStepImplementationDto> implementation();

    Optional<StrategyDto> strategy();

    @JsonProperty("x-$ref") Optional<String> xRef();

}

