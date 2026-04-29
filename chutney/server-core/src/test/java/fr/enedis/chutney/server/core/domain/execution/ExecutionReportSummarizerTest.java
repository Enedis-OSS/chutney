/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCoreBuilder;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExecutionReportSummarizerTest {

    private ExecutionReportSummarizer summarizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = mock(ObjectMapper.class);
        summarizer = new ExecutionReportSummarizer(objectMapper);
    }

    @Test
    public void should_summarize_execution_report_correctly() throws Exception {
        // Given
        Instant startDate = Instant.now();

        StepExecutionReportCore rootStep = new StepExecutionReportCoreBuilder()
            .setName("Test Step")
            .setStartDate(startDate)
            .setDuration(100L)
            .setStatus(ServerReportStatus.SUCCESS)
            .setInformation(Arrays.asList("Test executed successfully"))
            .createStepExecutionReport();

        ScenarioExecutionReport scenarioReport = new ScenarioExecutionReport(
            123L,
            "My Test Scenario",
            "production",
            "john.doe",
            Arrays.asList("tag1", "tag2"),
            null,
            rootStep
        );

        TestCase testCase = createTestCase();
        ExecutionRequest request = new ExecutionRequest(testCase, "production", "john.doe");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"report\":\"data\"}");

        // When
        ExecutionHistory.DetachedExecution result = summarizer.summarize(scenarioReport, request);

        // Then
        assertThat(result.status()).isEqualTo(ServerReportStatus.SUCCESS);
        assertThat(result.duration()).isEqualTo(100L);
        assertThat(result.testCaseTitle()).isEqualTo("My Test Scenario");
        assertThat(result.environment()).isEqualTo("production");
        assertThat(result.user()).isEqualTo("john.doe");
        assertThat(result.info()).hasValue("Test executed successfully");
        assertThat(result.error()).hasValue("");
        assertThat(result.report()).isEqualTo("{\"report\":\"data\"}");
        assertThat(result.time()).isNotNull();
    }

    @Test
    public void should_summarize_with_errors_only_from_failed_steps() throws Exception {
        // Given
        Instant startDate = Instant.now();

        // Step A: Has errors in its error list (from retries) but final status is SUCCESS
        StepExecutionReportCore stepA = new StepExecutionReportCoreBuilder()
            .setName("Step A with retry strategy")
            .setStartDate(startDate)
            .setDuration(100L)
            .setStatus(ServerReportStatus.SUCCESS)
            .setErrors(Arrays.asList("Step A - Retry attempt 1 failed", "Step A - Retry attempt 2 failed"))
            .setInformation(Arrays.asList("Step A succeeded after retries"))
            .createStepExecutionReport();

        // Step B: Actually failed with errors
        StepExecutionReportCore stepB = new StepExecutionReportCoreBuilder()
            .setName("Step B")
            .setStartDate(startDate)
            .setDuration(50L)
            .setStatus(ServerReportStatus.FAILURE)
            .setErrors(Arrays.asList("Step B - Fatal error occurred"))
            .createStepExecutionReport();

        // Root step: Failed because Step B failed
        StepExecutionReportCore rootStep = new StepExecutionReportCoreBuilder()
            .setName("Scenario root")
            .setStartDate(startDate)
            .setDuration(150L)
            .setStatus(ServerReportStatus.FAILURE)
            .setSteps(Arrays.asList(stepA, stepB))
            .setErrors(Collections.emptyList())
            .createStepExecutionReport();

        ScenarioExecutionReport scenarioReport = new ScenarioExecutionReport(
            1L,
            "Test Scenario",
            "test-env",
            "test-user",
            Collections.emptyList(),
            null,
            rootStep
        );

        TestCase testCase = createTestCase();
        ExecutionRequest request = new ExecutionRequest(testCase, "test-env", "test-user");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        ExecutionHistory.DetachedExecution result = summarizer.summarize(scenarioReport, request);

        // Then
        assertThat(result.status()).isEqualTo(ServerReportStatus.FAILURE);
        assertThat(result.error()).hasValue("Step B - Fatal error occurred");
        assertThat(result.error().get()).doesNotContain("Step A - Retry attempt");
        assertThat(result.duration()).isEqualTo(150L);
        assertThat(result.testCaseTitle()).isEqualTo("Test Scenario");
        assertThat(result.environment()).isEqualTo("test-env");
        assertThat(result.user()).isEqualTo("test-user");
    }

    @Test
    public void should_summarize_with_nested_failed_step_errors() throws Exception {
        // Given
        Instant startDate = Instant.now();

        StepExecutionReportCore deeplyNestedFailedStep = new StepExecutionReportCoreBuilder()
            .setName("Deeply nested failed step")
            .setStartDate(startDate)
            .setDuration(30L)
            .setStatus(ServerReportStatus.FAILURE)
            .setErrors(Arrays.asList("Connection timeout after 30s"))
            .createStepExecutionReport();

        StepExecutionReportCore parentWithRetry = new StepExecutionReportCoreBuilder()
            .setName("Parent step with retries")
            .setStartDate(startDate)
            .setDuration(100L)
            .setStatus(ServerReportStatus.FAILURE)
            .setErrors(Arrays.asList("Parent - Retry 1 error", "Parent - Retry 2 error"))
            .setSteps(Arrays.asList(deeplyNestedFailedStep))
            .createStepExecutionReport();

        StepExecutionReportCore rootStep = new StepExecutionReportCoreBuilder()
            .setName("Root scenario")
            .setStartDate(startDate)
            .setDuration(100L)
            .setStatus(ServerReportStatus.FAILURE)
            .setSteps(Arrays.asList(parentWithRetry))
            .setErrors(Collections.emptyList())
            .createStepExecutionReport();

        ScenarioExecutionReport scenarioReport = new ScenarioExecutionReport(
            1L,
            "Test Scenario",
            "test-env",
            "test-user",
            Collections.emptyList(),
            null,
            rootStep
        );

        TestCase testCase = createTestCase();
        ExecutionRequest request = new ExecutionRequest(testCase, "test-env", "test-user");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        ExecutionHistory.DetachedExecution result = summarizer.summarize(scenarioReport, request);

        // Then
        assertThat(result.status()).isEqualTo(ServerReportStatus.FAILURE);
        assertThat(result.error()).isPresent();
        // Should contain one of the errors from failed steps
        String error = result.error().get();
        assertThat(error).matches(".*(?:Connection timeout after 30s|Parent - Retry \\d error).*");
    }

    @Test
    public void should_summarize_with_direct_errors_when_present() throws Exception {
        // Given
        Instant startDate = Instant.now();

        StepExecutionReportCore rootStep = new StepExecutionReportCoreBuilder()
            .setName("Step with direct errors")
            .setStartDate(startDate)
            .setDuration(50L)
            .setStatus(ServerReportStatus.FAILURE)
            .setErrors(Arrays.asList("Direct error 1", "Direct error 2"))
            .createStepExecutionReport();

        ScenarioExecutionReport scenarioReport = new ScenarioExecutionReport(
            1L,
            "Test Scenario",
            "test-env",
            "test-user",
            Collections.emptyList(),
            null,
            rootStep
        );

        TestCase testCase = createTestCase();
        ExecutionRequest request = new ExecutionRequest(testCase, "test-env", "test-user");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        ExecutionHistory.DetachedExecution result = summarizer.summarize(scenarioReport, request);

        // Then
        assertThat(result.error()).hasValue("Direct error 1");
    }

    @Test
    public void should_summarize_successful_execution_without_errors() throws Exception {
        // Given
        Instant startDate = Instant.now();

        StepExecutionReportCore successStep = new StepExecutionReportCoreBuilder()
            .setName("Success step")
            .setStartDate(startDate)
            .setDuration(50L)
            .setStatus(ServerReportStatus.SUCCESS)
            .setInformation(Arrays.asList("All good"))
            .createStepExecutionReport();

        ScenarioExecutionReport scenarioReport = new ScenarioExecutionReport(
            1L,
            "Test Scenario",
            "test-env",
            "test-user",
            Collections.emptyList(),
            null,
            successStep
        );

        TestCase testCase = createTestCase();
        ExecutionRequest request = new ExecutionRequest(testCase, "test-env", "test-user");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        ExecutionHistory.DetachedExecution result = summarizer.summarize(scenarioReport, request);

        // Then
        assertThat(result.status()).isEqualTo(ServerReportStatus.SUCCESS);
        assertThat(result.error()).hasValue("");
        assertThat(result.info()).hasValue("All good");
    }

    @Test
    public void should_summarize_with_info_from_nested_steps() throws Exception {
        // Given
        Instant startDate = Instant.now();

        StepExecutionReportCore subStep1 = new StepExecutionReportCoreBuilder()
            .setName("Sub step 1")
            .setStartDate(startDate)
            .setDuration(20L)
            .setStatus(ServerReportStatus.SUCCESS)
            .setInformation(Arrays.asList("Info 1"))
            .createStepExecutionReport();

        StepExecutionReportCore subStep2 = new StepExecutionReportCoreBuilder()
            .setName("Sub step 2")
            .setStartDate(startDate)
            .setDuration(30L)
            .setStatus(ServerReportStatus.SUCCESS)
            .setInformation(Arrays.asList("Info 2"))
            .createStepExecutionReport();

        StepExecutionReportCore parentStep = new StepExecutionReportCoreBuilder()
            .setName("Parent")
            .setStartDate(startDate)
            .setDuration(50L)
            .setStatus(ServerReportStatus.SUCCESS)
            .setSteps(Arrays.asList(subStep1, subStep2))
            .setInformation(Collections.emptyList())
            .createStepExecutionReport();

        ScenarioExecutionReport scenarioReport = new ScenarioExecutionReport(
            1L,
            "Test Scenario",
            "test-env",
            "test-user",
            Collections.emptyList(),
            null,
            parentStep
        );

        TestCase testCase = createTestCase();
        ExecutionRequest request = new ExecutionRequest(testCase, "test-env", "test-user");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        ExecutionHistory.DetachedExecution result = summarizer.summarize(scenarioReport, request);

        // Then
        assertThat(result.status()).isEqualTo(ServerReportStatus.SUCCESS);
        assertThat(result.info()).hasValue("Info 1, Info 2");
    }

    @Test
    public void should_truncate_long_info_messages() throws Exception {
        // Given
        Instant startDate = Instant.now();

        String longInfo = "This is a very long information message that should be truncated because it exceeds the maximum length allowed";
        StepExecutionReportCore rootStep = new StepExecutionReportCoreBuilder()
            .setName("Step")
            .setStartDate(startDate)
            .setDuration(50L)
            .setStatus(ServerReportStatus.SUCCESS)
            .setInformation(Arrays.asList(longInfo))
            .createStepExecutionReport();

        ScenarioExecutionReport scenarioReport = new ScenarioExecutionReport(
            1L,
            "Test Scenario",
            "test-env",
            "test-user",
            Collections.emptyList(),
            null,
            rootStep
        );

        TestCase testCase = createTestCase();
        ExecutionRequest request = new ExecutionRequest(testCase, "test-env", "test-user");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        ExecutionHistory.DetachedExecution result = summarizer.summarize(scenarioReport, request);

        // Then
        assertThat(result.info()).isPresent();
        assertThat(result.info().get()).hasSizeLessThanOrEqualTo(53); // 50 + "..."
        assertThat(result.info().get()).endsWith("...");
    }

    private TestCase createTestCase() {
        TestCase testCase = mock(TestCase.class);
        when(testCase.id()).thenReturn("test-case-1");
        TestCaseMetadataImpl metadata = TestCaseMetadataImpl.builder()
            .withTitle("Test Case")
            .build();
        when(testCase.metadata()).thenReturn(metadata);
        return testCase;
    }
}
