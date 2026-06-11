/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.config.web.WebConfiguration;
import fr.enedis.chutney.scenario.api.raw.mapper.GwtTestCaseMapper;
import fr.enedis.chutney.scenario.api.raw.mapper.RawTestCaseMapper;
import fr.enedis.chutney.scenario.domain.gwt.GwtScenario;
import fr.enedis.chutney.scenario.domain.gwt.GwtStep;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class ImmutablesDtoSerializationTest {

    private final JsonMapper objectMapper = (JsonMapper) new WebConfiguration().webObjectMapper();

    @Test
    void should_serialize_gwt_test_case_dto_with_all_metadata_fields() throws Exception {
        GwtTestCase testCase = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder()
                .withId("1")
                .withTitle("titre")
                .withDescription("a testcase")
                .withTags(List.of("FIRST", "SECOND"))
                .withAuthor("robert")
                .withVersion(111)
                .withCreationDate(Instant.parse("2020-01-01T12:00:03Z"))
                .build())
            .withScenario(GwtScenario.builder().withWhen(GwtStep.builder().build()).build())
            .build();

        String json = objectMapper.writeValueAsString(GwtTestCaseMapper.toDto(testCase));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("title").asString()).isEqualTo("titre");
        assertThat(node.get("description").asString()).isEqualTo("a testcase");
        assertThat(node.get("author").asString()).isEqualTo("robert");
        assertThat(node.get("version").asInt()).isEqualTo(111);
        assertThat(node.get("tags").get(0).asString()).isEqualTo("FIRST");
        assertThat(node.has("scenario")).isTrue();
    }

    @Test
    void should_serialize_raw_test_case_dto_with_all_metadata_fields() throws Exception {
        GwtTestCase testCase = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder()
                .withId("1234")
                .withTitle("My new title")
                .withDescription("My new scenario description")
                .withTags(List.of("A_TAG"))
                .build())
            .withScenario(GwtScenario.builder().withWhen(GwtStep.builder().build()).build())
            .build();

        String json = objectMapper.writeValueAsString(RawTestCaseMapper.toDto(testCase));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("title").asString()).isEqualTo("My new title");
        assertThat(node.get("description").asString()).isEqualTo("My new scenario description");
        assertThat(node.get("tags").get(0).asString()).isEqualTo("A_TAG");
        assertThat(node.has("content")).isTrue();
    }
}
