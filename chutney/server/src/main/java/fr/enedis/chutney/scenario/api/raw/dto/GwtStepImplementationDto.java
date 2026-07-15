/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtStepImplementationDto.class)
@Value.Style(jdkOnly = true)
public interface GwtStepImplementationDto {

    @JsonProperty("task")
    @Value.Default
    default String task() {
        return "";
    }

    @JsonProperty("type")
    @Value.Default
    default String type() {
        return "";
    }

    @JsonProperty("target")
    @Value.Default
    default String target() {
        return "";
    }

    @Value.Default
    @JsonProperty("x-$ref")
    default String xRef() {
        return "";
    }

    @JsonProperty("inputs")
    Map<String, Object> inputs();

    @JsonProperty("outputs")
    Map<String, Object> outputs();

    @JsonProperty("validations")
    Map<String, Object> validations();

    @JsonCreator
    static GwtStepImplementationDto of(
        @JsonProperty("task") @Nullable String task,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("target") @Nullable String target,
        @JsonProperty("x-$ref") @Nullable String xRef,
        @JsonProperty("inputs") @Nullable Map<String, Object> inputs,
        @JsonProperty("outputs") @Nullable Map<String, Object> outputs,
        @JsonProperty("validations") @Nullable Map<String, Object> validations
    ) {
        ImmutableGwtStepImplementationDto.Builder builder = ImmutableGwtStepImplementationDto.builder();
        if (task != null) builder.task(task);
        if (type != null) builder.type(type);
        if (target != null) builder.target(target);
        if (xRef != null) builder.xRef(xRef);
        if (inputs != null) builder.inputs(inputs);
        if (outputs != null) builder.outputs(outputs);
        if (validations != null) builder.validations(validations);
        return builder.build();
    }
}
