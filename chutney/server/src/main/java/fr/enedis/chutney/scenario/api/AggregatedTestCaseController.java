/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api;

import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import fr.enedis.chutney.scenario.api.raw.dto.TestCaseIndexDto;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AggregatedTestCaseController.BASE_URL)
public class AggregatedTestCaseController {

    public static final String BASE_URL = "/api/scenario/v2";
    private final TestCaseRepository testCaseRepository;
    private final ExecutionHistoryRepository executionHistoryRepository;

    public AggregatedTestCaseController(TestCaseRepository testCaseRepository, ExecutionHistoryRepository executionHistoryRepository) {
        this.testCaseRepository = testCaseRepository;
        this.executionHistoryRepository = executionHistoryRepository;
    }

    @PreAuthorize("hasAnyAuthority('SCENARIO_READ', 'EXECUTION_READ')")
    @GetMapping(path = "/{testCaseId}/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public TestCaseIndexDto testCaseMetaData(@PathVariable("testCaseId") String testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId).orElseThrow(() -> new ScenarioNotFoundException(testCaseId));
        return TestCaseIndexDto.from(testCase.metadata());
    }

    @PreAuthorize("hasAnyAuthority('SCENARIO_READ', 'CAMPAIGN_READ', 'EXECUTION_READ')")
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TestCaseIndexDto> getTestCases() {
        List<TestCaseMetadata> testCases = testCaseRepository.findAll();
        Map<String, ExecutionSummary> lastExecutions = executionHistoryRepository.getLastExecutions(testCases.stream().map(TestCaseMetadata::id).collect(Collectors.toList()));

        return testCases.stream()
            .map((tc) -> {
                if (lastExecutions.get(tc.id()) != null) {
                    ExecutionSummaryDto execution = ExecutionSummaryDto.toDto(lastExecutions.get(tc.id()));
                    return TestCaseIndexDto.from(tc, execution);
                } else {
                    return TestCaseIndexDto.from(tc);
                }
            })
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @DeleteMapping(path = "/{testCaseId}")
    public void removeScenarioById(@PathVariable("testCaseId") String testCaseId) {
        testCaseRepository.removeById(testCaseId);
    }
}
