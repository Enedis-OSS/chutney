/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.api.dto;

import fr.enedis.chutney.dataset.api.DataSetMapper;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.util.List;
import java.util.stream.Collectors;

public class CampaignExecutionReportMapper {

    public static CampaignExecutionReportDto toDto(CampaignExecution campaignReport) {
        return new CampaignExecutionReportDto(
            campaignReport.executionId,
            campaignReport.scenarioExecutionReports().stream()
                .map(ScenarioExecutionReportCampaignMapper::toDto)
                .collect(Collectors.toList()),
            campaignReport.campaignName,
            campaignReport.startDate,
            campaignReport.status(),
            campaignReport.partialExecution,
            campaignReport.executionEnvironment,
            DataSetMapper.toDto(campaignReport.dataset),
            campaignReport.userId,
            campaignReport.getDuration());
    }

    public static CampaignExecutionFullReportDto fullExecutionToDto(CampaignExecution campaignReport, List<ExecutionHistory.Execution> executions) {
        return new CampaignExecutionFullReportDto(
            campaignReport.executionId,
            executions,
            campaignReport.campaignName,
            campaignReport.startDate,
            campaignReport.status(),
            campaignReport.partialExecution,
            campaignReport.executionEnvironment,
            campaignReport.userId,
            campaignReport.getDuration());
    }
}
