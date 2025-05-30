/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import static java.time.Instant.now;

import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import fr.enedis.chutney.server.core.domain.security.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtTestCaseDto.class)
@JsonDeserialize(as = ImmutableGwtTestCaseDto.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value.Style(jdkOnly = true)
public interface GwtTestCaseDto {

    Optional<String> id();

    String title();

    Optional<String> description();

    Optional<String> repositorySource();

    List<String> tags();

    List<ExecutionSummaryDto> executions();

    Optional<Instant> creationDate();

    GwtScenarioDto scenario();

    Optional<String> defaultDataset();

    @Value.Default()
    default String author() {
        return User.ANONYMOUS.id;
    }

    @Value.Default()
    default Instant updateDate() {
        return now();
    }

    @Value.Default()
    default Integer version() {
        return 1;
    }
}
