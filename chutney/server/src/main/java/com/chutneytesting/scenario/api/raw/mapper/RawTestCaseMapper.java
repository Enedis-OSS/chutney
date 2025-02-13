/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.scenario.api.raw.mapper;

import static org.hjson.JsonValue.readHjson;

import com.chutneytesting.execution.domain.GwtScenarioMarshaller;
import com.chutneytesting.scenario.api.raw.dto.ImmutableRawTestCaseDto;
import com.chutneytesting.scenario.api.raw.dto.RawTestCaseDto;
import com.chutneytesting.scenario.domain.gwt.GwtScenario;
import com.chutneytesting.scenario.domain.gwt.GwtTestCase;
import com.chutneytesting.server.core.domain.scenario.ScenarioNotParsableException;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadataImpl;
import org.hjson.Stringify;

public class RawTestCaseMapper {

    private static final GwtScenarioMarshaller marshaller = new GwtScenarioMapper();

    // DTO -> RawTestCase
    public static GwtTestCase fromDto(RawTestCaseDto dto) {
        String jsonScenario = formatContentToJson(dto.scenario());
        GwtScenario gwtScenario = marshaller.deserialize(dto.title(), dto.description().orElse(""), jsonScenario);
        return GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder()
                .withId(dto.id().orElse(null))
                .withTitle(dto.title())
                .withDescription(dto.description().orElse(null))
                .withTags(dto.tags())
                .withCreationDate(dto.creationDate())
                .withAuthor(dto.author())
                .withUpdateDate(dto.updateDate())
                .withVersion(dto.version())
                .withDefaultDataset(dto.defaultDataset().orElse(null))
                .build())
            .withScenario(gwtScenario)
            .build();
    }

    private static String formatContentToJson(String content) {
        try {
            return readHjson(content).toString();
        } catch (Exception e) {
            throw new ScenarioNotParsableException("Malformed json or hjson format. ", e);
        }
    }

    public static RawTestCaseDto toDto(GwtTestCase testCase) {
        return ImmutableRawTestCaseDto.builder()
            .id(testCase.metadata().id())
            .title(testCase.metadata().title())
            .description(testCase.metadata().description())
            .scenario(readHjson(marshaller.serialize(testCase.scenario)).toString(Stringify.HJSON))
            .tags(testCase.metadata().tags())
            .creationDate(testCase.metadata().creationDate())
            .author(testCase.metadata.author)
            .updateDate(testCase.metadata.updateDate)
            .version(testCase.metadata.version)
            .defaultDataset(testCase.metadata.defaultDataset)
            .build();
    }

}
