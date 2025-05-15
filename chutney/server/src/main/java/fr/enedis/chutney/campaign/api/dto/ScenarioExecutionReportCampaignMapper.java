/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.api.dto;


import fr.enedis.chutney.server.core.domain.scenario.campaign.ScenarioExecutionCampaign;

public class ScenarioExecutionReportCampaignMapper {

    public static ScenarioExecutionReportOutlineDto toDto(ScenarioExecutionCampaign scenarioReport) {
        return new ScenarioExecutionReportOutlineDto(
            scenarioReport.scenarioId(),
            scenarioReport.scenarioName(),
            scenarioReport.execution()
        );
    }

    public static ScenarioExecutionCampaign fromDto(ScenarioExecutionReportOutlineDto dto) {
        return new ScenarioExecutionCampaign(
            dto.getScenarioId(),
            dto.getScenarioName(),
            dto.getExecution()
        );
    }

}
