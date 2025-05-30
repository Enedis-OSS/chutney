/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.infrastructure.delegation;

import fr.enedis.chutney.engine.api.execution.StatusDto;
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReportBuilder;
import java.util.stream.Collectors;

class StepExecutionReportMapper {

    private StepExecutionReportMapper() {
    }

    static StepExecutionReport fromDto(StepExecutionReportDto reportDto) {
        return new StepExecutionReportBuilder().setName(reportDto.name)
            .setDuration(reportDto.duration)
            .setStartDate(reportDto.startDate)
            .setStatus(StatusMapper.fromDto(reportDto.status))
            .setInformation(reportDto.information)
            .setErrors(reportDto.errors)
            .setSteps(reportDto.steps.stream().map(StepExecutionReportMapper::fromDto).collect(Collectors.toList()))
            .setEvaluatedInputs(reportDto.context.evaluatedInputs)
            .setScenarioContext(reportDto.context.scenarioContext)
            .setStepResults(reportDto.context.stepResults)
            .setEvaluatedInputsSnapshot(reportDto.context.evaluatedInputs)
            .setStepResultsSnapshot(reportDto.context.evaluatedInputs)
            .setType(reportDto.type)
            .setTargetName(reportDto.targetName)
            .setTargetUrl(reportDto.targetUrl)
            .setStrategy(reportDto.strategy)
            .createStepExecutionReport();
    }

    private static class StatusMapper {
        static Status fromDto(StatusDto status) {
            return Status.valueOf(status.name());
        }
    }
}
