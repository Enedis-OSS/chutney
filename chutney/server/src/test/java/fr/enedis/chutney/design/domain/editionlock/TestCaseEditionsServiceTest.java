/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.domain.editionlock;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import java.time.Instant;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCaseEditionsServiceTest {

    private TestCaseEditionsService sut;

    private final TestCaseEditions testCaseEditions = mock(TestCaseEditions.class);
    private final TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);

    @BeforeEach
    public void before() {
        when(testCaseEditions.findBy(any())).thenCallRealMethod(); // Default method
        sut = new TestCaseEditionsService(testCaseEditions, testCaseRepository);
    }

    @Test
    public void should_retrieve_all_testcase_editions() {
        // Given
        String testCaseId = "testCaseId";
        TestCaseEdition testCaseEditionOne = buildEdition(testCaseId, 1, now().minusSeconds(5), "editor");
        TestCaseEdition testCaseEditionTwo = buildEdition(testCaseId, 5, now(), "another editor");
        List<TestCaseEdition> currentEditions = Lists.list(
            testCaseEditionOne,
            buildEdition("id", 1, now(), "editor"),
            testCaseEditionTwo
        );
        when(testCaseEditions.findAll()).thenReturn(currentEditions);

        // When
        List<TestCaseEdition> editions = sut.getTestCaseEditions(testCaseId);

        // Then
        assertThat(editions).containsExactlyInAnyOrder(testCaseEditionOne, testCaseEditionTwo);
    }

    @Test
    public void should_end_testcase_edition_for_user() {
        // Given
        String testCaseId = "testCaseId";
        String editor = "editor";
        TestCaseEdition editionToEnd = buildEdition(testCaseId, 1, now(), "editor");
        List<TestCaseEdition> editions = Lists.list(
            editionToEnd,
            buildEdition("id", 1, now(), "editor"),
            buildEdition(testCaseId, 1, now(), "another editor")
        );
        when(testCaseEditions.findAll()).thenReturn(editions);

        // When
        sut.endTestCaseEdition(testCaseId, editor);

        // Then
        verify(testCaseEditions).remove(editionToEnd);
    }

    @Test
    public void cannot_edit_more_than_one_time_same_testcase_for_given_user() {
        // Given
        String testCaseId = "testCaseId";
        String editor = "editor";
        Instant editionStartDate = now().minusSeconds(50);
        TestCaseEdition firstEdition = buildEdition(testCaseId, 1, editionStartDate, editor);
        List<TestCaseEdition> editions = Lists.list(firstEdition);
        when(testCaseEditions.findAll()).thenReturn(editions);

        // When
        TestCaseEdition testCaseEdition = sut.editTestCase(testCaseId, editor);

        // Then
        assertThat(testCaseEdition).isEqualTo(firstEdition);
    }

    @Test
    public void should_add_testcase_edition_for_user() {
        // Given
        Instant testStartDate = now();
        String testCaseId = "testCaseId";
        String editor = "editor";
        TestCaseMetadata metadataEditionToAdd = buildMetadataForEdition(testCaseId, 4);
        when(testCaseEditions.findAll()).thenReturn(emptyList());
        when(testCaseRepository.findById(testCaseId)).thenReturn(
            of(testCaseFromMetadata(metadataEditionToAdd))
        );
        when(testCaseEditions.add(any())).thenReturn(true);

        // When
        TestCaseEdition testCaseEditionAdded = sut.editTestCase(testCaseId, editor);

        // Then
        assertThat(testCaseEditionAdded.testCaseMetadata).isEqualTo(metadataEditionToAdd);
        assertThat(testCaseEditionAdded.editor).isEqualTo(editor);
        assertThat(testCaseEditionAdded.startDate).isAfterOrEqualTo(testStartDate);
    }

    @Test
    public void should_throw_exception_when_cannot_add_testcase_edition() {
        // Given
        String testCaseId = "testCaseId";
        when(testCaseEditions.findAll()).thenReturn(emptyList());
        when(testCaseRepository.findById(testCaseId)).thenReturn(
            of(testCaseFromMetadata(buildMetadataForEdition(testCaseId, 4)))
        );
        when(testCaseEditions.add(any())).thenReturn(false);

        // When
        assertThatThrownBy(() -> sut.editTestCase(testCaseId, "editor"))
            .isInstanceOf(IllegalStateException.class);
    }

    private TestCaseEdition buildEdition(String testCaseId, Integer version, Instant startDate, String editor) {
        return new TestCaseEdition(
            buildMetadataForEdition(testCaseId, version),
            startDate,
            editor
        );
    }

    private TestCaseMetadata buildMetadataForEdition(String testCaseId, Integer version) {
        return TestCaseMetadataImpl.builder()
            .withId(testCaseId)
            .withVersion(version)
            .build();
    }

    private TestCase testCaseFromMetadata(TestCaseMetadata metadata) {
        return () -> metadata;
    }
}
