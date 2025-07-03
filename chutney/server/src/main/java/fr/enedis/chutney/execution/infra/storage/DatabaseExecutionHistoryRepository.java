/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.storage;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enedis.chutney.campaign.infra.CampaignExecutionJpaRepository;
import fr.enedis.chutney.campaign.infra.CampaignJpaRepository;
import fr.enedis.chutney.campaign.infra.jpa.CampaignExecutionEntity;
import fr.enedis.chutney.execution.infra.storage.index.ExecutionReportIndexRepository;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.DetachedExecution;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.Execution;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.PurgeReport;
import fr.enedis.chutney.server.core.domain.execution.report.ReportNotFoundException;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
class DatabaseExecutionHistoryRepository implements ExecutionHistoryRepository {

    private final DatabaseExecutionJpaRepository scenarioExecutionsJpaRepository;
    private final ScenarioExecutionReportJpaRepository scenarioExecutionReportJpaRepository;
    private final CampaignJpaRepository campaignJpaRepository;
    private final CampaignExecutionJpaRepository campaignExecutionJpaRepository;
    private final TestCaseRepository testCaseRepository;
    private final ExecutionReportIndexRepository executionReportIndexRepository;
    private final ObjectMapper objectMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseExecutionHistoryRepository.class);


    DatabaseExecutionHistoryRepository(
        DatabaseExecutionJpaRepository scenarioExecutionsJpaRepository,
        ScenarioExecutionReportJpaRepository scenarioExecutionReportJpaRepository,
        CampaignJpaRepository campaignJpaRepository, TestCaseRepository testCaseRepository,
        CampaignExecutionJpaRepository campaignExecutionJpaRepository,
        ExecutionReportIndexRepository executionReportIndexRepository,
        @Qualifier("reportObjectMapper") ObjectMapper objectMapper) {
        this.scenarioExecutionsJpaRepository = scenarioExecutionsJpaRepository;
        this.scenarioExecutionReportJpaRepository = scenarioExecutionReportJpaRepository;
        this.campaignJpaRepository = campaignJpaRepository;
        this.testCaseRepository = testCaseRepository;
        this.campaignExecutionJpaRepository = campaignExecutionJpaRepository;
        this.executionReportIndexRepository = executionReportIndexRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, ExecutionSummary> getLastExecutions(List<String> scenariosIds) {
        List<String> validScenariosIds = scenariosIds.stream().filter(id -> !invalidScenarioId(id)).toList();
        List<Long> executionsIds = scenarioExecutionsJpaRepository.findLastByStatusAndScenariosIds(validScenariosIds, ServerReportStatus.RUNNING)
            .stream().map(t -> t.get(0, Long.class)).toList();
        Iterable<ScenarioExecutionEntity> lastExecutions = scenarioExecutionsJpaRepository.findAllById(executionsIds);
        return StreamSupport.stream(lastExecutions.spliterator(), true)
            .collect(Collectors.toMap(ScenarioExecutionEntity::scenarioId, ScenarioExecutionEntity::toDomain));
    }

    @Override
    public List<ExecutionSummary> getExecutions(String scenarioId) {
        if (invalidScenarioId(scenarioId)) {
            return emptyList();
        }
        List<ScenarioExecutionEntity> scenarioExecutions = scenarioExecutionsJpaRepository.findByScenarioIdOrderByIdDesc(scenarioId);
        return scenarioExecutions.stream()
            .map(this::scenarioExecutionToExecutionSummary)
            .toList();
    }

    @Override
    public List<ExecutionSummary> getExecutions() {
        return scenarioExecutionsJpaRepository.findAll().stream()
            .map(this::scenarioExecutionToExecutionSummary)
            .toList();
    }

    @Override
    public ExecutionSummary getExecutionSummary(Long executionId) {
        return scenarioExecutionsJpaRepository.findById(executionId)
            .map(this::scenarioExecutionToExecutionSummary)
            .orElseThrow(
                () -> new ReportNotFoundException(executionId)
            );
    }

    private ExecutionSummary scenarioExecutionToExecutionSummary(ScenarioExecutionEntity scenarioExecution) {
        CampaignExecution campaignExecution = ofNullable(scenarioExecution.campaignExecution())
            .map(ce -> ce.toDomain(campaignJpaRepository.findById(ce.campaignId()).get().title()))
            .orElse(null);
        return scenarioExecution.toDomain(campaignExecution);
    }

    @Override
    @Transactional
    public Execution store(String scenarioId, DetachedExecution detachedExecution) throws IllegalStateException {
        if (invalidScenarioId(scenarioId)) {
            throw new IllegalStateException("Scenario id is null or empty");
        }
        //TODO do not retrieve whole campaignExecution object. We already have the ID
        ScenarioExecutionEntity scenarioExecution = ScenarioExecutionEntity.fromDomain(scenarioId, detachedExecution);
        if (detachedExecution.campaignReport().isPresent()) {
            Optional<CampaignExecutionEntity> campaignExecution = campaignExecutionJpaRepository.findById(detachedExecution.campaignReport().get().executionId);
            scenarioExecution.forCampaignExecution(campaignExecution.get());
        }
        scenarioExecution = scenarioExecutionsJpaRepository.save(scenarioExecution);
        ScenarioExecutionReportEntity reportEntity = new ScenarioExecutionReportEntity(scenarioExecution, detachedExecution.report());
        scenarioExecutionReportJpaRepository.save(reportEntity);
        Execution execution = detachedExecution.attach(scenarioExecution.id(), scenarioId);
        return ImmutableExecutionHistory.Execution.builder().from(execution).build();
    }

    @Override
    // TODO remove scenarioId params
    public Execution getExecution(String scenarioId, Long reportId) throws ReportNotFoundException {
        if (invalidScenarioId(scenarioId) || testCaseRepository.findById(scenarioId).isEmpty()) {
            throw new ReportNotFoundException(scenarioId, reportId);
        }
        return scenarioExecutionReportJpaRepository.findById(reportId).map(ScenarioExecutionReportEntity::toDomain)
            .orElseThrow(
                () -> new ReportNotFoundException(scenarioId, reportId)
            );
    }

    @Override
    public List<ExecutionSummary> getExecutionReportMatchKeyword(String keyword) {
        List<Long> matchedReportsIds = executionReportIndexRepository.idsByKeywordInReport(keyword);
        return scenarioExecutionsJpaRepository
            .getExecutionReportByIds(matchedReportsIds)
            .stream()
            .map(this::scenarioExecutionToExecutionSummary)
            .toList();
    }

    @Override
    @Transactional
    public void update(String scenarioId, Execution updatedExecution) throws ReportNotFoundException {
        if (!scenarioExecutionsJpaRepository.existsById(updatedExecution.executionId())) {
            throw new ReportNotFoundException(scenarioId, updatedExecution.executionId());
        }
        update(updatedExecution);
    }

    private void update(Execution updatedExecution) throws ReportNotFoundException {
        ScenarioExecutionEntity execution = scenarioExecutionsJpaRepository.findById(updatedExecution.executionId()).orElseThrow(
            () -> new ReportNotFoundException(updatedExecution.executionId())
        );

        execution.updateFromExecution(updatedExecution);
        scenarioExecutionsJpaRepository.save(execution);
        updateReport(updatedExecution);
    }

    private void updateReport(Execution execution) throws ReportNotFoundException {
        ScenarioExecutionReportEntity scenarioExecutionReport = scenarioExecutionReportJpaRepository.findById(execution.executionId()).orElseThrow(
            () -> new ReportNotFoundException(execution.executionId())
        );
        scenarioExecutionReport.updateReport(execution);
        scenarioExecutionReportJpaRepository.save(scenarioExecutionReport);
    }

    @Override
    @Transactional
    public int setAllRunningExecutionsToKO() {
        List<ExecutionSummary> runningExecutions = getExecutionsWithStatus(ServerReportStatus.RUNNING);
        updateExecutionsToKO(runningExecutions);

        List<ExecutionSummary> pausedExecutions = getExecutionsWithStatus(ServerReportStatus.PAUSED);
        updateExecutionsToKO(pausedExecutions);

        return runningExecutions.size() + pausedExecutions.size();
    }

    @Override
    public List<ExecutionSummary> getExecutionsWithStatus(ServerReportStatus status) {
        return scenarioExecutionsJpaRepository.findByStatus(status).stream().map(ScenarioExecutionEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public PurgeReport deleteExecutions(Set<Long> executionsIds) {
        var finalScenariosExecutions = executionsIds.stream()
            .map(this::getExecutionSummary)
            .filter(executionSummary -> executionSummary.status().isFinal())
            .collect(toUnmodifiableSet());

        var toDeleteScenariosExecutionsIds = finalScenariosExecutions.stream()
            .map(ExecutionHistory.Attached::executionId)
            .collect(toUnmodifiableSet());

        var toDeleteCampaignsExecutionsIds = getCampaignExecutionsToDelete(toDeleteScenariosExecutionsIds);

        campaignExecutionJpaRepository.deleteAllByIdInBatch(toDeleteCampaignsExecutionsIds);
        scenarioExecutionReportJpaRepository.deleteAllById(toDeleteScenariosExecutionsIds);
        scenarioExecutionsJpaRepository.deleteAllByIdInBatch(toDeleteScenariosExecutionsIds);

        return new PurgeReport(toDeleteScenariosExecutionsIds, toDeleteCampaignsExecutionsIds);
    }

    private Set<Long> getCampaignExecutionsToDelete(Set<Long> executionsIds) {
        return executionsIds.stream()
            .map(this::getExecutionSummary)
            .map(ExecutionSummary::campaignReport)
            .flatMap(Optional::stream)
            .filter(campaignExecution -> campaignExecution.scenarioExecutionReports().size() == 1)
            .map(campaignExecution -> campaignExecution.executionId)
            .collect(Collectors.toSet());
    }

    private void updateExecutionsToKO(List<ExecutionSummary> executions) {
        executions.stream()
            .map(this::buildKnockoutExecutionFrom)
            .forEach(this::update);
    }

    private ImmutableExecutionHistory.Execution buildKnockoutExecutionFrom(ExecutionSummary executionSummary) {
        String reportStoppedRunningOrPausedStatus = stopRunningOrPausedReport(executionSummary);
        return ImmutableExecutionHistory.Execution.builder()
            .executionId(executionSummary.executionId())
            .status(ServerReportStatus.FAILURE)
            .time(executionSummary.time())
            .duration(executionSummary.duration())
            .info(executionSummary.info())
            .error("Execution was interrupted !")
            .report(reportStoppedRunningOrPausedStatus)
            .testCaseTitle(executionSummary.testCaseTitle())
            .environment(executionSummary.environment())
            .user(executionSummary.user())
            .scenarioId(executionSummary.scenarioId())
            .tags(executionSummary.tags())
            .build();
    }

    private String stopRunningOrPausedReport(ExecutionSummary executionSummary) {
        return scenarioExecutionReportJpaRepository.findById(executionSummary.executionId()).map(ScenarioExecutionReportEntity::toDomain).map(execution -> {
            try {
                ScenarioExecutionReport newScenarioExecutionReport = updateStatusInScenarioExecutionReportWithStoppedStatusIfRunningOrPaused(execution);
                return objectMapper.writeValueAsString(newScenarioExecutionReport);
            } catch (JsonProcessingException exception) {
                LOGGER.error("Unexpected error while deserializing report for execution id " + executionSummary.executionId(), exception);
                return "";
            }
        }).orElseGet(() -> {
            LOGGER.warn("Report not found for execution id {}", executionSummary.executionId());
            return "";
        });
    }

    private ScenarioExecutionReport updateStatusInScenarioExecutionReportWithStoppedStatusIfRunningOrPaused(Execution execution) throws JsonProcessingException {
        ScenarioExecutionReport scenarioExecutionReport = objectMapper.readValue(execution.report(), ScenarioExecutionReport.class);
        StepExecutionReportCore report = updateStepWithStoppedStatusIfRunningOrPaused(scenarioExecutionReport.report);
        return updateScenarioExecutionReport(scenarioExecutionReport, report);
    }

    private ScenarioExecutionReport updateScenarioExecutionReport(ScenarioExecutionReport scenarioExecutionReport, StepExecutionReportCore report) {
        return new ScenarioExecutionReport(
            scenarioExecutionReport.executionId,
            scenarioExecutionReport.scenarioName,
            scenarioExecutionReport.environment,
            scenarioExecutionReport.user,
            scenarioExecutionReport.tags,
            scenarioExecutionReport.datasetId,
            scenarioExecutionReport.constants,
            scenarioExecutionReport.datatable,
            report);
    }

    private List<StepExecutionReportCore> updateStepListWithStoppedStatusIfRunningOrPaused(List<StepExecutionReportCore> steps) {
        return steps.stream().map(this::updateStepWithStoppedStatusIfRunningOrPaused).collect(Collectors.toList());
    }

    private boolean isExecutionRunningOrPaused(ServerReportStatus status) {
        return status.equals(ServerReportStatus.RUNNING) || status.equals(ServerReportStatus.PAUSED);
    }

    private StepExecutionReportCore updateStepWithStoppedStatusIfRunningOrPaused(StepExecutionReportCore step) {
        ServerReportStatus status = isExecutionRunningOrPaused(step.status) ? ServerReportStatus.STOPPED : step.status;
        List<StepExecutionReportCore> steps = updateStepListWithStoppedStatusIfRunningOrPaused(step.steps);
        return new StepExecutionReportCore(
            step.name,
            step.duration,
            step.startDate,
            status,
            step.information,
            step.errors,
            steps,
            step.type,
            step.targetName,
            step.targetUrl,
            step.strategy,
            step.evaluatedInputs,
            step.stepOutputs
        );
    }

    private boolean invalidScenarioId(String scenarioId) {
        return isNullOrEmpty(scenarioId);
    }
}
