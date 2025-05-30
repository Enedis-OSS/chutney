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
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtStepImplementationDto.class)
@JsonDeserialize(as = ImmutableGwtStepImplementationDto.class)
@Value.Style(jdkOnly = true)
public interface GwtStepImplementationDto {

    @Value.Default
    default String task() {
        return "";
    }

    @Value.Default
    default String type() {
        return "";
    }

    @Value.Default
    default String target() {
        return "";
    }

    @Value.Default
    @JsonProperty("x-$ref")
    default String xRef() {
        return "";
    }

    Map<String, Object> inputs();

    Map<String, Object> outputs();

    Map<String, Object> validations();

}
