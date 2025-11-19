/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import static fr.enedis.chutney.jira.domain.XrayStatus.FAIL;
import static fr.enedis.chutney.jira.domain.XrayStatus.PASS;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.jira.domain.JiraRepository;
import fr.enedis.chutney.jira.domain.JiraServerConfiguration;
import fr.enedis.chutney.jira.domain.JiraXrayApi;
import fr.enedis.chutney.jira.domain.JiraXrayClientFactory;
import fr.enedis.chutney.jira.domain.JiraXrayService;
import fr.enedis.chutney.jira.infra.JiraFileRepository;
import fr.enedis.chutney.jira.xrayapi.Xray;
import fr.enedis.chutney.jira.xrayapi.XrayTest;
import fr.enedis.chutney.jira.xrayapi.XrayTestExecTest;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class JiraXrayEmbeddedApiTest {

    private final JiraXrayApi jiraXrayApiMock = mock(JiraXrayApi.class);
    private final JiraXrayClientFactory jiraXrayFactory = mock(JiraXrayClientFactory.class);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
    private final JiraServerConfiguration jiraServerConfiguration = new JiraServerConfiguration("an url", "a username", "a password", null, null, null);

    private JiraXrayEmbeddedApi jiraXrayEmbeddedApi;
    private JiraRepository jiraRepository;

    @BeforeEach
    public void setUp() throws IOException {
        jiraRepository = new JiraFileRepository(Files.createTempDirectory("jira").toString());
        jiraRepository.saveServerConfiguration(jiraServerConfiguration);

        JiraXrayService jiraXrayService = new JiraXrayService(jiraRepository, jiraXrayFactory);

        when(jiraXrayFactory.create(any())).thenReturn(jiraXrayApiMock);

        jiraXrayEmbeddedApi = new JiraXrayEmbeddedApi(jiraXrayService);
    }

    @Test
    void getTestStatus() {
        List<XrayTestExecTest> result = new ArrayList<>();
        XrayTestExecTest xrayTestExecTest = new XrayTestExecTest();
        xrayTestExecTest.setId("12345");
        xrayTestExecTest.setKey("SCE-2");
        xrayTestExecTest.setStatus(PASS.value);
        result.add(xrayTestExecTest);

        XrayTestExecTest xrayTestExecTest2 = new XrayTestExecTest();
        xrayTestExecTest2.setId("123456");
        xrayTestExecTest2.setKey("SCE-1");
        xrayTestExecTest2.setStatus(FAIL.value);
        result.add(xrayTestExecTest2);

        when(jiraXrayApiMock.getTestExecutionScenarios(anyString())).thenReturn(result);

        List<XrayTestExecTest> statusByTest = jiraXrayEmbeddedApi.getTestStatusInTestExec("");
        assertThat(statusByTest.getFirst().getStatus()).isEqualTo(PASS.value);
        assertThat(statusByTest.get(1).getStatus()).isEqualTo(FAIL.value);
    }

    @Test
    @DisplayName("Given an execution report, When we want to send the result to jira xray, Then the xray model api is filled with information from the report")
    void updateTestExecution() {
        // G
        jiraRepository.saveForCampaign("20", "JIRA-20");
        jiraRepository.saveForScenario("1", "SCE-1");
        ReportForJira.Step subStep = new ReportForJira.Step("sub step", of("Sub step error 1", "Sub step error 2"), null);
        ReportForJira.Step rootStep = new ReportForJira.Step("rootStep", of("Root error"), of(subStep));
        ReportForJira report = new ReportForJira(Instant.parse("2021-05-19T11:22:33.00Z"), 10000L, "SUCCESS", rootStep, "env");

        //W
        jiraXrayEmbeddedApi.updateTestExecution(new ExecutionJiraLink(20L, 1L, "1", "", null), report);

        //T
        ArgumentCaptor<Xray> xrayArgumentCaptor = ArgumentCaptor.forClass(Xray.class);
        verify(jiraXrayApiMock, times(1)).updateRequest(xrayArgumentCaptor.capture());

        Xray xrayValue = xrayArgumentCaptor.getValue();
        assertThat(xrayValue.getTestExecutionKey()).isEqualTo("JIRA-20");
        XrayTest xrayTest = xrayValue.getTests().getFirst();
        assertThat(xrayTest.getTestKey()).isEqualTo("SCE-1");
        assertThat(xrayTest.getStart()).isEqualTo(Instant.parse("2021-05-19T11:22:33.00Z").atZone(ZoneId.systemDefault()).format(formatter));
        assertThat(xrayTest.getFinish()).isEqualTo(Instant.parse("2021-05-19T11:22:43.00Z").atZone(ZoneId.systemDefault()).format(formatter));
        assertThat(xrayTest.getComment()).isEqualTo("[ > rootStep > sub step => [Sub step error 1, Sub step error 2],  > rootStep => [Root error]]");
        assertThat(xrayTest.getStatus()).isEqualTo(PASS.value);
    }

    @Test
    @DisplayName("Given an execution report, When we want to send the result to jira xray, Then the campaign execution key is overridden by th execution key if the latter is filled")
    void updateTestExecutionOverridingExecutionKey() {
        // G
        jiraRepository.saveForCampaign("20", "JIRA-20");
        jiraRepository.saveForScenario("1", "SCE-1");
        ReportForJira.Step subStep = new ReportForJira.Step("sub step", of("Sub step error 1", "Sub step error 2"), null);
        ReportForJira.Step rootStep = new ReportForJira.Step("rootStep", of("Root error"), of(subStep));
        ReportForJira report = new ReportForJira(Instant.parse("2021-05-19T11:22:33.00Z"), 10000L, "SUCCESS", rootStep, "env");

        //W
        String jiraId = "JIRA-125";
        jiraXrayEmbeddedApi.updateTestExecution(new ExecutionJiraLink(20L, 1L, "1", "", jiraId), report);

        //T
        ArgumentCaptor<Xray> xrayArgumentCaptor = ArgumentCaptor.forClass(Xray.class);
        verify(jiraXrayApiMock, times(1)).updateRequest(xrayArgumentCaptor.capture());

        Xray xrayValue = xrayArgumentCaptor.getValue();
        assertThat(xrayValue.getTestExecutionKey()).isEqualTo("JIRA-125");
    }

    @Test
    @DisplayName("Given an execution report, When we want to send the result to jira xray using test plan id, Then new test execution are created")
    void updateTestExecutionWithTestPlan() {
        // G
        jiraRepository.saveForCampaign("20", "JIRA-20");
        jiraRepository.saveForScenario("1", "SCE-1");
        ReportForJira.Step subStep = new ReportForJira.Step("sub step", of("Sub step error 1", "Sub step error 2"), null);
        ReportForJira.Step rootStep = new ReportForJira.Step("rootStep", of("Root error"), of(subStep));
        ReportForJira report = new ReportForJira(Instant.parse("2021-05-19T11:22:33.00Z"), 10000L, "SUCCESS", rootStep, "env");

        //W
        when(jiraXrayApiMock.isTestPlan("JIRA-20")).thenReturn(true);
        when(jiraXrayApiMock.createTestExecution("JIRA-20")).thenReturn("JIRA-22");
        jiraXrayEmbeddedApi.updateTestExecution(new ExecutionJiraLink(20L, 1L, "1", "", null), report);

        //T
        ArgumentCaptor<Xray> xrayArgumentCaptor = ArgumentCaptor.forClass(Xray.class);
        verify(jiraXrayApiMock, times(1)).updateRequest(xrayArgumentCaptor.capture());

        Xray xrayValue = xrayArgumentCaptor.getValue();
        assertThat(xrayValue.getTestExecutionKey()).isEqualTo("JIRA-22");
        jiraRepository.getByCampaignExecutionId("1").equals("JIRA-22");
    }

    @Test
    @DisplayName("Given an execution report, When we want to send the result to jira xray using not linked dataset scenario, Then jira test exec not updated")
    void doNotUpdateTestExecutionWhenUsingNotLinkedDatasetScenario() {
        // G
        jiraRepository.saveForCampaign("20", "JIRA-20");
        jiraRepository.saveDatasetForScenario("1", Map.of("dataset-01", "Test-1"));
        ReportForJira.Step subStep = new ReportForJira.Step("sub step", of("Sub step error 1", "Sub step error 2"), null);
        ReportForJira.Step rootStep = new ReportForJira.Step("rootStep", of("Root error"), of(subStep));
        ReportForJira report = new ReportForJira(Instant.parse("2021-05-19T11:22:33.00Z"), 10000L, "SUCCESS", rootStep, "env");

        //W
        when(jiraXrayApiMock.isTestPlan("JIRA-20")).thenReturn(true);
        when(jiraXrayApiMock.createTestExecution("JIRA-20")).thenReturn("JIRA-22");
        jiraXrayEmbeddedApi.updateTestExecution(new ExecutionJiraLink(20L, 1L, "1", "dataset-02", null), report);

        //T
        verify(jiraXrayApiMock, times(0)).updateRequest(any());
    }

    @ParameterizedTest
    @MethodSource("datatableListParameters")
    @DisplayName("Given an execution report, When we want to send the result to jira xray using dataset scenario link id, Then jira id linked to dataset and scenario are used")
    void updateTestExecutionUsingDatasetScenarioLink(String scenarioJiraId, Map<String, String> datasetJiraIdMap, String datasetUsed, String expectedTestKey) {
        // G
        jiraRepository.saveForCampaign("20", "JIRA-20");
        jiraRepository.saveForScenario("1", scenarioJiraId);
        jiraRepository.saveDatasetForScenario("1", datasetJiraIdMap);
        ReportForJira.Step subStep = new ReportForJira.Step("sub step", of("Sub step error 1", "Sub step error 2"), null);
        ReportForJira.Step rootStep = new ReportForJira.Step("rootStep", of("Root error"), of(subStep));
        ReportForJira report = new ReportForJira(Instant.parse("2021-05-19T11:22:33.00Z"), 10000L, "SUCCESS", rootStep, "env");

        //W
        when(jiraXrayApiMock.isTestPlan("JIRA-20")).thenReturn(true);
        when(jiraXrayApiMock.createTestExecution("JIRA-20")).thenReturn("JIRA-22");
        jiraXrayEmbeddedApi.updateTestExecution(new ExecutionJiraLink(20L, 1L, "1", datasetUsed, null), report);

        //T
        ArgumentCaptor<Xray> xrayArgumentCaptor = ArgumentCaptor.forClass(Xray.class);
        verify(jiraXrayApiMock, times(1)).updateRequest(xrayArgumentCaptor.capture());

        Xray xrayValue = xrayArgumentCaptor.getValue();
        assertThat(xrayValue.getTests().getFirst().getTestKey()).isEqualTo(expectedTestKey);
    }

    private static Object[] datatableListParameters() {
        return new Object[]{
            new Object[]{
                "SCE-1",//scenarioJiraId
                Map.of("dataset-01", "Test-1"),//datasetJiraIdMap,
                "dataset-01",//datasetUsed
                "Test-1"//expectedTestKey
            },
            new Object[]{
                "SCE-1",//scenarioJiraId
                Map.of("dataset-01", "Test-1"),//datasetJiraIdMap,
                "",//datasetUsed
                "SCE-1"//expectedTestKey
            },
            new Object[]{
                "SCE-1",//scenarioJiraId
                Map.of("dataset-01", "Test-1"),//datasetJiraIdMap,
                "dataset-02",//datasetUsed
                "SCE-1"//expectedTestKey
            },
            new Object[]{
                "",//scenarioJiraId
                Map.of("dataset-01", "Test-1"),//datasetJiraIdMap,
                "dataset-01",//datasetUsed
                "Test-1"//expectedTestKey
            }
        };
    }
}
