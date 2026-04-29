/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms scenario execution reports into execution history summaries.
 * Extracts errors, information, and metadata from step execution reports.
 */
public class ExecutionReportSummarizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionReportSummarizer.class);

    private final ObjectMapper reportObjectMapper;

    public ExecutionReportSummarizer(ObjectMapper reportObjectMapper) {
        this.reportObjectMapper = reportObjectMapper;
    }

    /**
     * Build a {@link ExecutionHistory.DetachedExecution} to store via ExecutionHistoryRepository
     *
     * @param scenarioReport report to summarize
     * @param executionRequest original execution request
     * @return detached execution ready to be stored
     */
    public ExecutionHistory.DetachedExecution summarize(ScenarioExecutionReport scenarioReport, ExecutionRequest executionRequest) {
        return ImmutableExecutionHistory.DetachedExecution.builder()
            .time(scenarioReport.report.startDate.atZone(ZoneId.systemDefault()).toLocalDateTime())
            .duration(scenarioReport.report.duration)
            .status(scenarioReport.report.status)
            .info(joinAndTruncateMessages(searchInfo(scenarioReport.report)))
            .error(searchErrors(scenarioReport.report).stream().findFirst().orElse(""))
            .report(serialize(scenarioReport))
            .testCaseTitle(scenarioReport.scenarioName)
            .environment(executionRequest.environment)
            .user(executionRequest.userId)
            .dataset(ofNullable(executionRequest.dataset))
            .build();
    }

    /**
     * Recursively search for error messages in failed steps only.
     * Ignores errors from steps that succeeded after retries.
     *
     * @param report the step execution report to search
     * @return list of error messages from failed steps
     */
    private List<String> searchErrors(StepExecutionReportCore report) {
        if (report.errors.isEmpty() && report.status != ServerReportStatus.SUCCESS) {
            return report.steps.stream()
                .filter(stepExecutionReportCore -> stepExecutionReportCore.status == ServerReportStatus.FAILURE)
                .map(this::searchErrors)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        } else {
            return report.errors;
        }
    }

    /**
     * Recursively search for information messages in step reports.
     *
     * @param report the step execution report to search
     * @return list of information messages
     */
    private List<String> searchInfo(StepExecutionReportCore report) {
        if (report.information.isEmpty()) {
            return report.steps.stream()
                .map(this::searchInfo)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        } else {
            return report.information;
        }
    }

    private String serialize(ScenarioExecutionReport stepExecutionReport) {
        try {
            return reportObjectMapper.writeValueAsString(stepExecutionReport);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to serialize StepExecutionReport content with name='{}'", stepExecutionReport.report.name, e);
            return "{}";
        }
    }

    private Optional<String> joinAndTruncateMessages(Iterable<String> messages) {
        return Optional.of(Ascii.truncate(Joiner.on(", ").useForNull("null").join(messages), 50, "...")).filter(s -> !s.isEmpty());
    }
}
