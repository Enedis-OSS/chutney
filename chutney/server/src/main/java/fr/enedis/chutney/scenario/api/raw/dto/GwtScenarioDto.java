/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtScenarioDto.class)
@JsonDeserialize(as = ImmutableGwtScenarioDto.class)
@Value.Style(jdkOnly = true)
public interface GwtScenarioDto {

    List<GwtStepDto> givens();

    GwtStepDto when();

    List<GwtStepDto> thens();

}
