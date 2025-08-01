/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.storage.jpa;

import static fr.enedis.chutney.execution.infra.execution.DatasetEntityMapper.getDataset;
import static fr.enedis.chutney.server.core.domain.dataset.DataSet.CUSTOM_ID;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.campaign.infra.jpa.CampaignExecutionEntity;
import fr.enedis.chutney.scenario.infra.raw.TagListMapper;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.ZoneId;
import org.apache.commons.lang3.StringUtils;

@Entity(name = "SCENARIO_EXECUTIONS")
public class ScenarioExecutionEntity {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SCENARIO_ID")
    private String scenarioId;

    @ManyToOne
    @JoinColumn(name = "CAMPAIGN_EXECUTION_ID")
    private CampaignExecutionEntity campaignExecution;

    @Column(name = "EXECUTION_TIME")
    private Long executionTime;

    @Column(name = "DURATION")
    private Long duration;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private ServerReportStatus status;

    @Column(name = "INFORMATION")
    private String information;

    @Column(name = "ERROR")
    private String error;

    @Column(name = "SCENARIO_TITLE")
    private String scenarioTitle;

    @Column(name = "ENVIRONMENT")
    private String environment;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "TAGS")
    private String tags;

    @Column(name = "DATASET_ID")
    private String datasetId;

    @Column(name = "VERSION")
    @Version
    private Integer version;

    public ScenarioExecutionEntity() {
    }

    public ScenarioExecutionEntity(
        Long id,
        String scenarioId,
        CampaignExecutionEntity campaignExecution,
        Long executionTime,
        Long duration,
        ServerReportStatus status,
        String information,
        String error,
        String scenarioTitle,
        String environment,
        String userId,
        String datasetId,
        String tags,
        Integer version
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.campaignExecution = campaignExecution;
        this.executionTime = executionTime;
        this.duration = duration;
        this.status = status;
        this.information = information;
        this.error = error;
        this.scenarioTitle = scenarioTitle;
        this.environment = environment;
        this.userId = userId;
        this.datasetId = datasetId;
        this.tags = tags;
        this.version = version;
    }

    public Long id() {
        return id;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public CampaignExecutionEntity campaignExecution() {
        return campaignExecution;
    }

    public void forCampaignExecution(CampaignExecutionEntity campaignExecutionEntity) {
        this.campaignExecution = campaignExecutionEntity;
    }

    public void clearCampaignExecution() {
        this.campaignExecution = null;
    }

    public Integer version() {
        return version;
    }

    public Long executionTime() {
        return executionTime;
    }

    public Long duration() {
        return duration;
    }

    public ServerReportStatus status() {
        return status;
    }

    public String information() {
        return information;
    }

    public String error() {
        return error;
    }

    public String scenarioTitle() {
        return scenarioTitle;
    }

    public String environment() {
        return environment;
    }

    public String userId() {
        return userId;
    }

    public String datasetId() {
        return datasetId;
    }

    public String tags() {
        return tags;
    }

    public Long getId() {
        return id;
    }

    public static ScenarioExecutionEntity fromDomain(String scenarioId, ExecutionHistory.ExecutionProperties execution) {
        return fromDomain(scenarioId, null, null, execution);
    }

    public static ScenarioExecutionEntity fromDomain(String scenarioId, Long id, Integer version, ExecutionHistory.ExecutionProperties execution) {
        return new ScenarioExecutionEntity(
            id,
            scenarioId,
            null,
            execution.time().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            execution.duration(),
            execution.status(),
            execution.info().map(ScenarioExecutionEntity::truncateExecutionTrace).orElse(null),
            execution.error().map(ScenarioExecutionEntity::truncateExecutionTrace).orElse(null),
            execution.testCaseTitle(),
            execution.environment(),
            execution.user(),
            execution.dataset().map(ScenarioExecutionEntity::mapDataset).orElse(null),
            truncateExecutionTags(TagListMapper.tagsToString(execution.tags().orElse(null))),
            version
        );
    }

    private static String mapDataset(DataSet dataset) {
        if (dataset.id != null) return dataset.id;
        if (dataset.name != null && dataset.name.isEmpty()) return CUSTOM_ID;
        return null;
    }

    public ExecutionHistory.ExecutionSummary toDomain() {
        return toDomain(null);
    }

    public ExecutionHistory.ExecutionSummary toDomain(CampaignExecution campaignReport) {
        return ImmutableExecutionHistory.ExecutionSummary.builder()
            .executionId(id)
            .time(Instant.ofEpochMilli(executionTime).atZone(ZoneId.systemDefault()).toLocalDateTime())
            .duration(duration)
            .status(status)
            .info(ofNullable(information))
            .error(ofNullable(error))
            .testCaseTitle(scenarioTitle)
            .environment(environment)
            .dataset(ofNullable(getDataset(datasetId, null, null)))
            .user(userId)
            .campaignReport(ofNullable(campaignReport))
            .scenarioId(scenarioId)
            .tags(TagListMapper.tagsStringToSet(tags))
            .build();
    }

    public void updateFromExecution(ExecutionHistory.Execution execution) {
        duration = execution.duration();
        status = execution.status();
        information = execution.info().map(ScenarioExecutionEntity::truncateExecutionTrace).orElse(null);
        error = execution.error().map(ScenarioExecutionEntity::truncateExecutionTrace).orElse(null);
    }

    private static String truncateExecutionTrace(String trace) {
        return StringUtils.substring(trace, 0, 512);
    }

    private static String truncateExecutionTags(String tags) {
        return tags != null ? StringUtils.substring(tags, 0, 500) : null;
    }
}
