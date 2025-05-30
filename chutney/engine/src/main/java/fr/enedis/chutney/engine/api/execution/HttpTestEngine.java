/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.api.execution;

import io.reactivex.rxjava3.core.Observable;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes Scenario execution HTTP service.
 * Used by agents only for the moment
 */
@RestController
public class HttpTestEngine implements TestEngine {

    public static final String EXECUTION_URL = "/api/scenario/execution/v1";

    private final TestEngine testEngine;

    public HttpTestEngine(TestEngine embeddedTestEngine) {
        this.testEngine = embeddedTestEngine;
    }

    @Override
    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = EXECUTION_URL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StepExecutionReportDto execute(@Valid @RequestBody ExecutionRequestDto request) {
        return testEngine.execute(request);
    }

    @Override
    public Long executeAsync(ExecutionRequestDto request) {
        throw new IllegalArgumentException();
    }

    @Override
    public Observable<StepExecutionReportDto> receiveNotification(Long executionId) {
        throw new IllegalArgumentException();
    }

    @Override
    public void pauseExecution(Long executionId) {
        throw new IllegalArgumentException();
    }

    @Override
    public void resumeExecution(Long executionId) {
        throw new IllegalArgumentException();
    }

    @Override
    public void stopExecution(Long executionId) {
        throw new IllegalArgumentException();
    }

    @Override
    public void close() throws Exception {
        testEngine.close();
    }
}
