/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableTestCaseIndexDto.class)
public interface TestCaseIndexDto {

    @JsonUnwrapped
    GwtTestCaseMetadataDto metadata();

    @JsonCreator
    static TestCaseIndexDto of(@JsonUnwrapped GwtTestCaseMetadataDto metadata) {
        return ImmutableTestCaseIndexDto.builder().metadata(metadata).build();
    }

    static TestCaseIndexDto from(TestCaseMetadata testCaseMetadata) {
        return ImmutableTestCaseIndexDto.builder()
            .metadata(ImmutableGwtTestCaseMetadataDto.builder()
                .id(testCaseMetadata.id())
                .creationDate(testCaseMetadata.creationDate())
                .updateDate(testCaseMetadata.updateDate())
                .title(testCaseMetadata.title())
                .description(testCaseMetadata.description())
                .tags(testCaseMetadata.tags())
                .executions(emptyList())
                .build()
            )
            .build();
    }

    static TestCaseIndexDto from(TestCaseMetadata testCaseMetadata, ExecutionSummaryDto execution) {
        return ImmutableTestCaseIndexDto.builder()
            .metadata(ImmutableGwtTestCaseMetadataDto.builder()
                .id(testCaseMetadata.id())
                .creationDate(testCaseMetadata.creationDate())
                .updateDate(testCaseMetadata.updateDate())
                .title(testCaseMetadata.title())
                .description(testCaseMetadata.description())
                .tags(testCaseMetadata.tags())
                .executions(List.of(execution))
                .build()
            )
            .build();
    }
}
