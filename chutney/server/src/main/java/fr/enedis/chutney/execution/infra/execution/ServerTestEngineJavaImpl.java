/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.execution;

import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto;
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto;
import fr.enedis.chutney.engine.api.execution.TestEngine;
import fr.enedis.chutney.server.core.domain.execution.ExecutionRequest;
import fr.enedis.chutney.server.core.domain.execution.ServerTestEngine;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.tuple.Pair;

public class ServerTestEngineJavaImpl implements ServerTestEngine {

    private final TestEngine executionEngine;
    private final ExecutionRequestMapper executionRequestMapper;

    public ServerTestEngineJavaImpl(TestEngine executionEngine,
                                    ExecutionRequestMapper executionRequestMapper) {
        this.executionEngine = executionEngine;
        this.executionRequestMapper = executionRequestMapper;
    }

    @Override
    public StepExecutionReportCore execute(ExecutionRequest executionRequest) {
        ExecutionRequestDto executionRequestDto = executionRequestMapper.toDto(executionRequest);
        StepExecutionReportDto stepExecutionReportDto = executionEngine.execute(executionRequestDto);
        return StepExecutionReportMapperCore.fromDto(stepExecutionReportDto);
    }

    @Override
    public Pair<Observable<StepExecutionReportCore>, Long> executeAndFollow(ExecutionRequest executionRequest) {
        ExecutionRequestDto executionRequestDto = executionRequestMapper.toDto(executionRequest);
        Long executionId = executionEngine.executeAsync(executionRequestDto);
        return Pair.of(
            executionEngine.receiveNotification(executionId).map(StepExecutionReportMapperCore::fromDto),
            executionId
        );
    }

    @Override
    public void stop(Long executionHash) {
        executionEngine.stopExecution(executionHash);
    }

    @Override
    public void pause(Long executionHash) {
        executionEngine.pauseExecution(executionHash);
    }

    @Override
    public void resume(Long executionHash) {
        executionEngine.resumeExecution(executionHash);
    }
}
