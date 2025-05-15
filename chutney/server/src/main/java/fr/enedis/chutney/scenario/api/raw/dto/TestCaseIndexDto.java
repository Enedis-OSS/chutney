/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import static java.util.Collections.emptyList;

import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableTestCaseIndexDto.class)
@JsonDeserialize(as = ImmutableTestCaseIndexDto.class)
public interface TestCaseIndexDto {

    GwtTestCaseMetadataDto metadata();

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
