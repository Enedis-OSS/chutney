/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.mapper;

import fr.enedis.chutney.scenario.api.raw.dto.GwtTestCaseDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtTestCaseDto;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import java.util.Collections;

// TODO test me
public class GwtTestCaseMapper {

    // DTO -> TestCase
    public static GwtTestCase fromDto(GwtTestCaseDto dto) {
        return GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder()
                .withId(dto.id().orElse(null))
                .withTitle(dto.title())
                .withDescription(dto.description().orElse(null))
                .withTags(dto.tags())
                .withCreationDate(dto.creationDate().orElse(null))
                .withAuthor(dto.author())
                .withUpdateDate(dto.updateDate())
                .withVersion(dto.version())
                .withDefaultDataset(dto.defaultDataset().orElse(null))
                .build())
            .withScenario(GwtScenarioMapper.fromDto(dto.title(), dto.description().orElse(""), dto.scenario()))
            .build();
    }

    // TestCase -> DTO
    public static GwtTestCaseDto toDto(TestCase testCase) {
        if (testCase instanceof GwtTestCase) {
            return fromGwt((GwtTestCase) testCase);
        }
        throw new IllegalStateException("Bad format. " +
            "Test Case [" + testCase.metadata().id() + "] is not a GwtTestCase but a " + testCase.getClass().getSimpleName());
    }

    private static GwtTestCaseDto fromGwt(GwtTestCase testCase) {
        return ImmutableGwtTestCaseDto.builder()
            .id(testCase.metadata().id())
            .title(testCase.metadata().title())
            .description(testCase.metadata().description())
            .tags(testCase.metadata().tags())
            .executions(Collections.emptyList())
            .creationDate(testCase.metadata().creationDate())
            .scenario(GwtScenarioMapper.toDto(testCase.scenario))
            .author(testCase.metadata.author)
            .updateDate(testCase.metadata.updateDate)
            .version(testCase.metadata.version)
            .defaultDataset(testCase.metadata.defaultDataset)
            .build();
    }
}
