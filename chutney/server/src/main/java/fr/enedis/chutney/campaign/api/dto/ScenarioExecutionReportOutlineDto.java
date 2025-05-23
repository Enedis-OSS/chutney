/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.api.dto;

import static java.util.Collections.emptySet;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScenarioExecutionReportOutlineDto {
    private String scenarioId;
    private String scenarioName;
    private ExecutionHistory.ExecutionSummary execution;

    public ScenarioExecutionReportOutlineDto(String scenarioId,
                                             String scenarioName,
                                             ExecutionHistory.ExecutionSummary execution) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.execution = execution;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public Long getExecutionId() {
        return execution.executionId();
    }

    public long getDuration() {
        return execution.duration();
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public LocalDateTime getStartDate() {
        return execution.time();
    }

    public ServerReportStatus getStatus() {
        return execution.status();
    }

    public String getInfo() {
        return execution.info().orElse("");
    }

    public String getError() {
        return execution.error().orElse("");
    }

    public Set<String> getTags() {
        return execution.tags().orElse(emptySet());
    }
    public DataSet getDataset() {
        return execution.dataset().orElse(null);
    }

    ExecutionHistory.ExecutionSummary getExecution() {
        return execution;
    }
}
