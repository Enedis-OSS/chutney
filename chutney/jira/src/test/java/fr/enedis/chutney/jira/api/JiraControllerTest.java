/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import static fr.enedis.chutney.jira.domain.XrayStatus.PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.MapEntry.entry;
import static org.assertj.core.util.Lists.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enedis.chutney.jira.domain.JiraRepository;
import fr.enedis.chutney.jira.domain.JiraServerConfiguration;
import fr.enedis.chutney.jira.domain.JiraXrayApi;
import fr.enedis.chutney.jira.domain.JiraXrayClientFactory;
import fr.enedis.chutney.jira.domain.JiraXrayService;
import fr.enedis.chutney.jira.infra.JiraFileRepository;
import fr.enedis.chutney.jira.xrayapi.XrayTestExecTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JiraControllerTest {

    private JiraRepository jiraRepository;
    private MockMvc mockMvc;
    private final JiraXrayApi mockJiraXrayApi = mock(JiraXrayApi.class);
    private final JiraXrayClientFactory jiraXrayFactory = mock(JiraXrayClientFactory.class);
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    public void setUp() throws IOException {
        jiraRepository = new JiraFileRepository(Files.createTempDirectory("jira").toString());
        JiraXrayService jiraXrayService = new JiraXrayService(jiraRepository, jiraXrayFactory);

        when(jiraXrayFactory.create(any())).thenReturn(mockJiraXrayApi);

        jiraRepository.saveServerConfiguration(new JiraServerConfiguration("an url", "a username", "a password", null, null, null));
        jiraRepository.saveForCampaign("10", "JIRA-10");
        jiraRepository.saveForCampaign("20", "JIRA-20");
        jiraRepository.saveForScenario("1", "SCE-1");
        jiraRepository.saveForScenario("2", "SCE-2");
        jiraRepository.saveForScenario("3", "SCE-3");
        jiraRepository.saveDatasetForScenario("2", Map.of("dataset_1", "JIRA-02"));
        jiraRepository.saveDatasetForScenario("4", Map.of("dataset_1", "JIRA-04"));

        JiraController jiraController = new JiraController(jiraRepository, jiraXrayService);
        mockMvc = MockMvcBuilders.standaloneSetup(jiraController).build();
    }

    @Test
    void should_not_create_HttpJiraXrayImpl_if_url_not_exist() {
        jiraRepository.saveServerConfiguration(new JiraServerConfiguration("", "a username", "a password", null, null, null));

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> getJiraController("/api/ui/jira/v1/testexec/JIRA-10", new TypeReference<>() {
            }))
            .withMessageContaining("Cannot request xray server, jira url is undefined");
    }

    @Test
    void getLinkedScenarios() {
        Map<String, JiraScenarioLinksDto> map = getJiraController("/api/ui/jira/v1/scenario", new TypeReference<>() {
        });

        assertThat(map).hasSize(4);
        assertThat(map.get("1")).isEqualTo(
            ImmutableJiraScenarioLinksDto.builder().chutneyId("1").id("SCE-1").build()
        );
        assertThat(map.get("2")).isEqualTo(
            ImmutableJiraScenarioLinksDto.builder().chutneyId("2").id("SCE-2").datasetLinks(Map.of("dataset_1", "JIRA-02")).build()
        );
        assertThat(map.get("3")).isEqualTo(
            ImmutableJiraScenarioLinksDto.builder().chutneyId("3").id("SCE-3").build()
        );
        assertThat(map.get("4")).isEqualTo(
            ImmutableJiraScenarioLinksDto.builder().chutneyId("4").datasetLinks(Map.of("dataset_1", "JIRA-04")).build()
        );
    }

    @Test
    void getLinkedCampaigns() {
        Map<String, String> map = getJiraController("/api/ui/jira/v1/campaign", new TypeReference<>() {
        });

        assertThat(map).hasSize(2);
        assertThat(map).containsOnly(entry("10", "JIRA-10"), entry("20", "JIRA-20"));
    }

    @Test
    void getByScenarioId() {
        JiraScenarioLinksDto jiraDto = getJiraController("/api/ui/jira/v1/scenario/2", new TypeReference<>() {
        });

        assertThat(jiraDto.chutneyId()).isEqualTo("2");
        assertThat(jiraDto.id()).isEqualTo("SCE-2");
        assertThat(jiraDto.datasetLinks()).containsEntry("dataset_1", "JIRA-02");
    }

    @Test
    void saveForScenario() {
        JiraScenarioLinksDto dto = ImmutableJiraScenarioLinksDto.builder().chutneyId("123").id("SCE-123").datasetLinks(Map.of("dataset_01", "JIRA-1")).build();
        JiraScenarioLinksDto jiraDto = postJiraController("/api/ui/jira/v1/scenario", new TypeReference<>() {
        }, dto);

        assertThat(jiraDto.chutneyId()).isEqualTo("123");
        assertThat(jiraDto.id()).isEqualTo("SCE-123");
        assertThat(jiraDto.datasetLinks()).containsEntry("dataset_01", "JIRA-1");
        assertThat(jiraRepository.getAllLinkedScenarios()).contains(entry("123", "SCE-123"));
        assertThat(jiraRepository.getAllLinkedScenariosWithDataset()).contains(entry("123", Map.of("dataset_01", "JIRA-1")));
    }

    @Test
    void removeForScenario() {
        deleteJiraController("/api/ui/jira/v1/scenario/1");

        assertThat(jiraRepository.getByScenarioId("1")).isEmpty();
    }

    @Test
    void getByCampaignId() {
        JiraDto jiraDto = getJiraController("/api/ui/jira/v1/campaign/10", new TypeReference<>() {
        });

        assertThat(jiraDto.chutneyId()).isEqualTo("10");
        assertThat(jiraDto.id()).isEqualTo("JIRA-10");
    }

    @Test
    void getScenariosByTestExecutionId() {

        List<XrayTestExecTest> result = new ArrayList<>();
        XrayTestExecTest xrayTestExecTest = new XrayTestExecTest();
        xrayTestExecTest.setId("12345");
        xrayTestExecTest.setKey("SCE-2");
        xrayTestExecTest.setStatus(PASS.value);
        result.add(xrayTestExecTest);

        when(mockJiraXrayApi.getTestExecutionScenarios(anyString())).thenReturn(result);

        List<JiraDto> scenarios = getJiraController("/api/ui/jira/v1/testexec/JIRA-10", new TypeReference<>() {
        });

        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.getFirst().id()).isEqualTo("SCE-2");
        assertThat(scenarios.getFirst().chutneyId()).isEqualTo("2");
        assertThat(scenarios.getFirst().executionStatus().get()).isEqualTo(PASS.value);
    }

    @Test
    void getScenariosByCampaignExecutionId() {
        List<XrayTestExecTest> result = new ArrayList<>();
        XrayTestExecTest xrayTestExecTest = new XrayTestExecTest();
        xrayTestExecTest.setId("12345");
        xrayTestExecTest.setKey("SCE-2");
        xrayTestExecTest.setStatus(PASS.value);
        result.add(xrayTestExecTest);
        jiraRepository.saveForCampaignExecution("3", "JIRA-22");
        when(mockJiraXrayApi.getTestExecutionScenarios("JIRA-22")).thenReturn(result);

        JiraTestExecutionDto jiraTestExecution = getJiraController("/api/ui/jira/v1/campaign_execution/3", new TypeReference<>() {
        });

        assertThat(jiraTestExecution.jiraScenarios()).hasSize(1);
        assertThat(jiraTestExecution.jiraScenarios().getFirst().id()).isEqualTo("SCE-2");
        assertThat(jiraTestExecution.jiraScenarios().getFirst().chutneyId()).isEqualTo("2");
        assertThat(jiraTestExecution.jiraScenarios().getFirst().executionStatus().get()).isEqualTo(PASS.value);
    }

    @Test
    void saveForCampaign() {
        JiraDto dto = ImmutableJiraDto.builder().chutneyId("123").id("JIRA-123").build();
        JiraDto jiraDto = postJiraController("/api/ui/jira/v1/campaign", new TypeReference<>() {
        }, dto);

        assertThat(jiraDto.chutneyId()).isEqualTo("123");
        assertThat(jiraDto.id()).isEqualTo("JIRA-123");
        assertThat(jiraRepository.getAllLinkedCampaigns()).contains(entry("123", "JIRA-123"));
    }

    @Test
    void removeForCampaign() {
        deleteJiraController("/api/ui/jira/v1/campaign/10");

        assertThat(jiraRepository.getByCampaignId("10")).isEmpty();
    }

    @Test
    void getConfiguration() {
        JiraConfigurationDto configurationDto = getJiraController("/api/ui/jira/v1/configuration", new TypeReference<>() {
        });

        assertThat(configurationDto.url()).isEqualTo("an url");
        assertThat(configurationDto.username()).isEqualTo("a username");
        assertThat(configurationDto.password()).isEqualTo("a password");
    }

    @Test
    void getConfigurationUrl() throws Exception {
        String url = mockMvc.perform(MockMvcRequestBuilders.get("/api/ui/jira/v1/configuration/url")
                .accept(MediaType.TEXT_PLAIN_VALUE)) // <--
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(url).isEqualTo("an url");
    }

    @Test
    void loadServerConfigurationWithoutProxy() {

        JiraRepository repository = new JiraFileRepository(Paths.get("src", "test", "resources", "jira").toAbsolutePath().toString());
        JiraServerConfiguration expectedConfiguration = new JiraServerConfiguration("an url", "a username", "a password", "", "", "");

        JiraServerConfiguration expected = repository.loadServerConfiguration();

        assertThat(expected).usingRecursiveComparison().isEqualTo(expectedConfiguration);
    }

    @Test
    void saveConfiguration() throws Exception {
        JiraServerConfiguration newConfiguration = new JiraServerConfiguration("a new url", "a new username", "a new password", "url proxy", "user proxy", "password proxy");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/ui/jira/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(om.writeValueAsString(newConfiguration))
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk());

        JiraServerConfiguration expected = jiraRepository.loadServerConfiguration();
        assertThat(expected).usingRecursiveComparison().isEqualTo(newConfiguration);
    }

    @Test
    void deleteConfiguration() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/ui/jira/v1/configuration"))
            .andExpect(MockMvcResultMatchers.status().is(204));

        JiraServerConfiguration expected = jiraRepository.loadServerConfiguration();
        assertThat(expected).isNotNull();
        assertThat(expected.url()).isEmpty();
        assertThat(expected.username()).isEmpty();
        assertThat(expected.password()).isEmpty();
        assertThat(expected.urlProxy()).isEmpty();
        assertThat(expected.userProxy()).isEmpty();
        assertThat(expected.passwordProxy()).isEmpty();
    }

    @Test
    void updateStatus() throws Exception {
        JiraDto dto = ImmutableJiraDto.builder().chutneyId("3").id("").executionStatus(PASS.value).build();

        XrayTestExecTest xrayTestExecTest = new XrayTestExecTest();
        xrayTestExecTest.setId("runIdentifier");
        xrayTestExecTest.setKey("SCE-3");

        when(mockJiraXrayApi.getTestExecutionScenarios(anyString())).thenReturn(list(xrayTestExecTest));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/ui/jira/v1/testexec/JIRA-10")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(om.writeValueAsString(dto))
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(mockJiraXrayApi).updateStatusByTestRunId(eq("runIdentifier"), eq(PASS.value));
    }

    private <T> T getJiraController(String url, TypeReference<T> typeReference) {
        try {
            String contentAsString = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
            return om.readValue(contentAsString, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteJiraController(String url) {
        try {
            mockMvc.perform(MockMvcRequestBuilders.delete(url)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T postJiraController(String url, TypeReference<T> typeReference, Object object) {
        try {
            String contentAsString = mockMvc.perform(MockMvcRequestBuilders.post(url)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(om.writeValueAsString(object))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
            return om.readValue(contentAsString, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
