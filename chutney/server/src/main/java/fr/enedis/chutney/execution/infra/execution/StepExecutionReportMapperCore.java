/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.execution;

import static java.util.Collections.EMPTY_MAP;

import fr.enedis.chutney.engine.api.execution.StatusDto;
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCoreBuilder;
import java.util.stream.Collectors;

class StepExecutionReportMapperCore {

    private StepExecutionReportMapperCore() {
    }

    static StepExecutionReportCore fromDto(StepExecutionReportDto reportDto) {
        return new StepExecutionReportCoreBuilder()
            .setName(reportDto.name)
            .setDuration(reportDto.duration)
            .setStartDate(reportDto.startDate)
            .setStatus(ReportStatusMapper.fromDto(reportDto.status))
            .setInformation(reportDto.information)
            .setErrors(reportDto.errors)
            .setSteps(reportDto.steps.stream().map(StepExecutionReportMapperCore::fromDto).collect(Collectors.toList()))
            .setEvaluatedInputs(reportDto.context != null ? reportDto.context.evaluatedInputs : EMPTY_MAP)
            .setStepOutputs(reportDto.context != null ? reportDto.context.stepResults : EMPTY_MAP)
            .setType(reportDto.type)
            .setTargetName(reportDto.targetName)
            .setTargetUrl(reportDto.targetUrl)
            .setStrategy(reportDto.strategy)
            .createStepExecutionReport();
    }

    private static class ReportStatusMapper {
        public static ServerReportStatus fromDto(StatusDto status) {
            return ServerReportStatus.valueOf(status.name());
        }
    }
}
