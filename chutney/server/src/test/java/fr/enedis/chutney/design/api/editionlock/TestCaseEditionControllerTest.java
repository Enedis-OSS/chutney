/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.api.editionlock;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.enedis.chutney.RestExceptionHandler;
import fr.enedis.chutney.WebConfiguration;
import fr.enedis.chutney.design.domain.editionlock.TestCaseEdition;
import fr.enedis.chutney.design.domain.editionlock.TestCaseEditionsService;
import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.security.infra.SpringUserService;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class TestCaseEditionControllerTest {

    private final TestCaseEditionsService testCaseEditionService = mock(TestCaseEditionsService.class);
    private final SpringUserService userService = mock(SpringUserService.class);
    private MockMvc mockMvc;
    private final ObjectMapper om = new WebConfiguration().webObjectMapper();
    private final UserDto currentUser = new UserDto();

    @BeforeEach
    public void before() {
        TestCaseEditionController sut = new TestCaseEditionController(testCaseEditionService, userService);

        mockMvc = MockMvcBuilders.standaloneSetup(sut)
            .setControllerAdvice(new RestExceptionHandler(Mockito.mock(ChutneyMetrics.class)))
            .build();

        currentUser.setId("currentUser");
        when(userService.currentUser()).thenReturn(currentUser);
    }

    @Test
    public void should_get_testcase_editions() throws Exception {
        // Given
        String testCaseId = "testCaseId";
        TestCaseEdition firstTestCaseEdition = buildEdition(testCaseId, 2, now(), "editor");
        TestCaseEdition secondTestCaseEdition = buildEdition(testCaseId, 5, now().minusSeconds(45), "another editor");
        when(testCaseEditionService.getTestCaseEditions(testCaseId)).thenReturn(
            Lists.list(firstTestCaseEdition, secondTestCaseEdition)
        );

        // When
        MvcResult mvcResult = mockMvc.perform(get(TestCaseEditionController.BASE_URL + "/" + testCaseId))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        List<TestCaseEditionDto> result = om.readValue(
            mvcResult.getResponse().getContentAsString(),
            new TypeReference<>() {
            }
        );

        assertThat(result).hasSize(2);
        assertDtoIsEqualToEdition(result.getFirst(), firstTestCaseEdition);
        assertDtoIsEqualToEdition(result.get(1), secondTestCaseEdition);
    }

    @Test
    public void should_edit_testcase() throws Exception {
        // Given
        String testCaseId = "testCaseId";
        TestCaseEdition newTestCaseEdition = buildEdition(testCaseId, 2, now(), currentUser.getId());
        when(testCaseEditionService.editTestCase(testCaseId, currentUser.getId())).thenReturn(newTestCaseEdition);

        // When
        MvcResult mvcResult = mockMvc.perform(
            post(TestCaseEditionController.BASE_URL + "/" + testCaseId)
                .contentType(APPLICATION_JSON_VALUE)
                .content("")
        )
            .andExpect(status().isOk())
            .andReturn();

        // Then
        TestCaseEditionDto dto = om.readValue(mvcResult.getResponse().getContentAsString(), TestCaseEditionDto.class);
        assertDtoIsEqualToEdition(dto, newTestCaseEdition);
    }

    @Test
    public void should_end_testcase_edition() throws Exception {
        // Given
        String testCaseId = "testCaseId";

        // When
        MvcResult mvcResult = mockMvc.perform(delete(TestCaseEditionController.BASE_URL + "/" + testCaseId))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        verify(testCaseEditionService).endTestCaseEdition(testCaseId, currentUser.getId());
    }

    private void assertDtoIsEqualToEdition(TestCaseEditionDto dto, TestCaseEdition edition) {
        assertThat(dto.testCaseId()).isEqualTo(edition.testCaseMetadata.id());
        assertThat(dto.testCaseVersion()).isEqualTo(edition.testCaseMetadata.version());
        assertThat(dto.editionUser()).isEqualTo(edition.editor);
        assertThat(dto.editionStartDate()).isEqualTo(edition.startDate);
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
}
