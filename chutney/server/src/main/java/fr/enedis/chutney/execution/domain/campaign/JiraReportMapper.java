/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.domain.campaign;

import fr.enedis.chutney.jira.api.ReportForJira;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class JiraReportMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraReportMapper.class);

    public static ReportForJira from(String stringReport, ObjectMapper objectMapper) {
        try {
            ScenarioExecutionReport scenarioReport = objectMapper.readValue(stringReport, ScenarioExecutionReport.class);

            return new ReportForJira(
                scenarioReport.report.startDate,
                scenarioReport.report.duration,
                scenarioReport.report.status.name(),
                createStep(scenarioReport.report),
                scenarioReport.environment);

        } catch (Exception e) {
            LOGGER.error("Cannot deserialize scenarioReport", e);
            return null;
        }
    }

    private static ReportForJira.Step createStep(StepExecutionReportCore coreStep) {
        return new ReportForJira.Step(coreStep.name, coreStep.errors, coreStep.steps.stream().map(JiraReportMapper::createStep).collect(Collectors.toList()));
    }

}
