/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.api.dto;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class CampaignMapper {

    public static CampaignDto toDtoWithoutReport(Campaign campaign) {
        return new CampaignDto(
            campaign.id,
            campaign.title,
            campaign.description,
            campaign.scenarios.stream().map(CampaignMapper::toDto).toList(),
            emptyList(),
            campaign.executionEnvironment(),
            campaign.parallelRun,
            campaign.retryAuto,
            campaign.executionDataset(),
            campaign.tags);
    }

    public static CampaignDto toDto(Campaign campaign, List<CampaignExecution> campaignExecutions) {
        return new CampaignDto(
            campaign.id,
            campaign.title,
            campaign.description,
            campaign.scenarios.stream().map(CampaignMapper::toDto).toList(),
            reportToDto(campaignExecutions),
            campaign.executionEnvironment(),
            campaign.parallelRun,
            campaign.retryAuto,
            campaign.executionDataset(),
            campaign.tags);
    }

    public static Campaign fromDto(CampaignDto dto) {
        return new Campaign(
            dto.getId(),
            dto.getTitle(),
            dto.getDescription(),
            campaignScenariosFromDto(dto),
            dto.getEnvironment(),
            dto.isParallelRun(),
            dto.isRetryAuto(),
            !StringUtils.isBlank(dto.getDatasetId()) ? dto.getDatasetId() : null,
            dto.getTags().stream().map(String::trim).map(String::toUpperCase).collect(toList())
        );
    }

    public static CampaignDto.CampaignScenarioDto toDto(Campaign.CampaignScenario campaignScenario) {
        return new CampaignDto.CampaignScenarioDto(campaignScenario.scenarioId(), campaignScenario.datasetId());
    }

    public static Campaign.CampaignScenario fromDto(CampaignDto.CampaignScenarioDto dto) {
        return new Campaign.CampaignScenario(dto.scenarioId(), dto.datasetId());
    }

    private static List<CampaignExecutionReportDto> reportToDto(List<CampaignExecution> campaignExecutions) {
        return campaignExecutions != null ? campaignExecutions.stream()
            .map(CampaignExecutionReportMapper::toDto)
            .collect(toList()) : emptyList();
    }

    private static List<Campaign.CampaignScenario> campaignScenariosFromDto(CampaignDto dto) {
        return ofNullable(dto.getScenarios()).filter(not(List::isEmpty))
            .map(list -> list.stream().map(sc -> new Campaign.CampaignScenario(sc.scenarioId(), sc.datasetId())).toList())
            .orElse(emptyList());
    }
}
