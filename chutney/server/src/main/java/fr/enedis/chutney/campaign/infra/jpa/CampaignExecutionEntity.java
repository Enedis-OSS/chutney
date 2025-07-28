/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra.jpa;

import static fr.enedis.chutney.execution.infra.execution.DatasetEntityMapper.datasetConstantsToString;
import static fr.enedis.chutney.execution.infra.execution.DatasetEntityMapper.datasetDatatableToString;
import static fr.enedis.chutney.execution.infra.execution.DatasetEntityMapper.getDataset;
import static fr.enedis.chutney.server.core.domain.dataset.DataSet.CUSTOM_ID;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;

import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecutionReportBuilder;
import fr.enedis.chutney.server.core.domain.scenario.campaign.ScenarioExecutionCampaign;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "CAMPAIGN_EXECUTIONS")
public class CampaignExecutionEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CAMPAIGN_ID")
    private Long campaignId;

    @OneToMany(mappedBy = "campaignExecution")
    private List<ScenarioExecutionEntity> scenarioExecutions;

    @Column(name = "PARTIAL")
    private Boolean partial;

    @Column(name = "ENVIRONMENT")
    private String environment;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "DATASET_ID")
    private String datasetId;

    @Column(name = "DATASET_CONSTANTS")
    private String datasetConstants;

    @Column(name = "DATASET_DATATABLE")
    private String datasetDatatable;

    @Column(name = "VERSION")
    @Version
    private Integer version;

    public CampaignExecutionEntity() {
    }

    public CampaignExecutionEntity(Long campaignId, String environment, DataSet dataset) {
        this(null, campaignId, null, null, environment, null, dataset, null);
    }

    private CampaignExecutionEntity(Long id, Long campaignId, List<ScenarioExecutionEntity> scenarioExecutions, Boolean partial, String environment, String userId, DataSet dataset, Integer version) {
        this.id = id;
        this.campaignId = campaignId;
        this.scenarioExecutions = scenarioExecutions;
        this.partial = ofNullable(partial).orElse(false);
        this.environment = environment;
        this.userId = userId;
        computeDataset(dataset);
        this.version = version;
    }

    public Long id() {
        return id;
    }

    public Long campaignId() {
        return campaignId;
    }

    public List<ScenarioExecutionEntity> scenarioExecutions() {
        return scenarioExecutions;
    }

    public String environment() {
        return environment;
    }

    public void updateFromDomain(CampaignExecution report, Iterable<ScenarioExecutionEntity> scenarioExecutions) {
        partial = report.partialExecution;
        environment = report.executionEnvironment;
        userId = report.userId;
        computeDataset(report.dataset);
        ofNullable(this.scenarioExecutions).ifPresent(see -> {
            see.clear();
            scenarioExecutions.forEach(se -> {
                se.forCampaignExecution(this);
                see.add(se);
            });
        });
    }

    public CampaignExecution toDomain(String campaignTitle) {
        List<ScenarioExecutionCampaign> scenarioExecutionReports = ofNullable(scenarioExecutions).map(see ->
            see.stream()
                .map(se -> new ScenarioExecutionCampaign(se.scenarioId(), se.scenarioTitle(), se.toDomain()))
                .collect(toCollection(ArrayList::new))
        ).orElseGet(ArrayList::new);

        CampaignExecutionReportBuilder campaignExecutionReportBuilder = CampaignExecutionReportBuilder.builder()
            .executionId(id)
            .campaignId(campaignId)
            .campaignName(campaignTitle)
            .partialExecution(ofNullable(partial).orElse(false))
            .environment(environment)
            .dataset(getDataset(datasetId, datasetConstants, datasetDatatable))
            .userId(userId);

        if (scenarioExecutionReports.isEmpty()) {
            campaignExecutionReportBuilder.status(ServerReportStatus.SUCCESS).startDate(LocalDateTime.MIN);
        } else {
            campaignExecutionReportBuilder.scenarioExecutionReport(scenarioExecutionReports);
        }

        return campaignExecutionReportBuilder.build();
    }

    private void computeDataset(DataSet dataset) {
        ofNullable(dataset).ifPresent(ds -> {
            this.datasetId = mapDataset(ds);
            this.datasetConstants = datasetConstantsToString(ds.constants);
            this.datasetDatatable = datasetDatatableToString(ds.datatable);
        });
    }

    private static String mapDataset(DataSet dataset) {
        if (dataset.id != null) return dataset.id;
        if (dataset.name != null && dataset.name.isEmpty()) return CUSTOM_ID;
        return null;
    }
}
