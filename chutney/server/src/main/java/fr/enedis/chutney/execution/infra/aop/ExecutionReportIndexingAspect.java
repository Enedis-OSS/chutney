/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.aop;

import fr.enedis.chutney.execution.infra.storage.DatabaseExecutionJpaRepository;
import fr.enedis.chutney.execution.infra.storage.index.ExecutionReportIndexRepository;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExecutionReportIndexingAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionReportIndexingAspect.class);
    private final ExecutionReportIndexRepository reportIndexRepository;
    private final DatabaseExecutionJpaRepository scenarioExecutionRepository;

    public ExecutionReportIndexingAspect(ExecutionReportIndexRepository reportIndexRepository, DatabaseExecutionJpaRepository scenarioExecutionRepository) {
        this.reportIndexRepository = reportIndexRepository;
        this.scenarioExecutionRepository = scenarioExecutionRepository;
    }

    @After("execution(* fr.enedis.chutney.execution.infra.storage.ScenarioExecutionReportJpaRepository.save(..)) && args(reportEntity)")
    public void index(ScenarioExecutionReportEntity reportEntity) {
        try {
            if (reportEntity.status().isFinal()) {
                reportIndexRepository.save(reportEntity);
            }
        } catch (Exception e) {
            LOGGER.error("Error when indexing execution report: ", e);
        }
    }

    @After("execution(* fr.enedis.chutney.scenario.infra.raw.ScenarioJpaRepository.save(..)) && args(scenario)")
    public void deleteDeactivatedScenarioExecutions(ScenarioEntity scenario) {
        try {
            if (!scenario.isActivated()) {
                List<ScenarioExecutionEntity> executions = scenarioExecutionRepository.findAllByScenarioId(String.valueOf(scenario.getId()));
                reportIndexRepository.deleteAllById(executions.stream().map(ScenarioExecutionEntity::getId).collect(Collectors.toSet()));
            }
        } catch (Exception e) {
            LOGGER.error("Error when deleting deactivated execution report index: ", e);
        }
    }

    @After("execution(* fr.enedis.chutney.execution.infra.storage.ScenarioExecutionReportJpaRepository.deleteAllById(..)) && args(scenarioExecutionIds)")
    public void deleteById(Set<Long> scenarioExecutionIds) {
        try {
            reportIndexRepository.deleteAllById(scenarioExecutionIds);
        } catch (Exception e) {
            LOGGER.error("Error when deleting execution report index: ", e);
        }
    }
}
