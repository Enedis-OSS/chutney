/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtTestCaseMetadataDto.class)
@JsonDeserialize(as = ImmutableGwtTestCaseMetadataDto.class)
public interface GwtTestCaseMetadataDto {

    Optional<String> id();

    String title();

    Optional<String> description();

    Optional<String> repositorySource();

    List<String> tags();

    List<ExecutionSummaryDto> executions();

    @Value.Default()
    default Instant creationDate() {
        return Instant.now();
    }

    @Value.Default()
    default Instant updateDate() {
        return Instant.now();
    }

}
