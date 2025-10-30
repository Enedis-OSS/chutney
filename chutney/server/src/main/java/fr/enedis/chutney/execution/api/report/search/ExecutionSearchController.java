/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api.report.search;

import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/execution")
public class ExecutionSearchController {

    private final ExecutionHistoryRepository executionHistoryRepository;

    ExecutionSearchController(
        ExecutionHistoryRepository executionHistoryRepository
    ) {
        this.executionHistoryRepository = executionHistoryRepository;
    }

    @PreAuthorize("hasAuthority('EXECUTION_READ')")
    @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ExecutionSummaryDto> getExecutionReportMatchQuery(@QueryParam("query") String query) {
        return executionHistoryRepository.getExecutionReportMatchKeyword(query).stream().map(ExecutionSummaryDto::toDto).toList();
    }
}

