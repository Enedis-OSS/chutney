/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.config.web.WebConfiguration;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AggregatedTestCaseControllerTest {

    private final JsonMapper om = (JsonMapper) new WebConfiguration().webObjectMapper();
    private final ExecutionHistoryRepository executionHistoryRepository = mock(ExecutionHistoryRepository.class);
    private final TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        AggregatedTestCaseController testCaseController = new AggregatedTestCaseController(testCaseRepository, executionHistoryRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(testCaseController)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(om))
            .build();
    }

    @Test
    public void should_call_repository_to_get_scenario_metadata() throws Exception {
        // Given
        TestCase mockTestCase = mock(TestCase.class);
        String id = "1";
        TestCaseMetadata fakeMetadata = TestCaseMetadataImpl.builder().withId(id).build();
        when(mockTestCase.metadata()).thenReturn(fakeMetadata);
        when(testCaseRepository.findById(eq(id))).thenReturn(of(mockTestCase));

        //When
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/scenario/v2/1/metadata")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        //Then
        verify(testCaseRepository).findById(eq(id));

        JsonNode metadata = om.readTree(mvcResult.getResponse().getContentAsString());
        assertThat(metadata.get("id").asText()).isEqualTo(fakeMetadata.id());
        assertThat(metadata.get("title").asText()).isEqualTo(fakeMetadata.title());
    }

    @Test
    public void should_call_repository_to_get_all_scenarios_metadata() throws Exception {
        // Given
        String id = "1";
        TestCaseMetadata fakeMetadata = TestCaseMetadataImpl.builder().withId(id).build();
        when(testCaseRepository.findAll()).thenReturn(List.of(fakeMetadata));

        //When
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/scenario/v2")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        //Then
        verify(testCaseRepository).findAll();

        JsonNode metadata = om.readTree(mvcResult.getResponse().getContentAsString()).get(0);
        assertThat(metadata.get("id").asText()).isEqualTo(fakeMetadata.id());
        assertThat(metadata.get("title").asText()).isEqualTo(fakeMetadata.title());
    }

    @Test
    public void should_call_repository_to_delete_scenario() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/scenario/v2/1")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk());
        verify(testCaseRepository).removeById(eq("1"));
    }
}
