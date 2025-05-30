/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api;

import static fr.enedis.chutney.dataset.api.DataSetMapper.fromExecutionDatasetDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.enedis.chutney.dataset.api.ExecutionDatasetDto;
import fr.enedis.chutney.dataset.domain.DataSetRepository;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.execution.domain.GwtScenarioMarshaller;
import fr.enedis.chutney.scenario.api.raw.mapper.GwtScenarioMapper;
import fr.enedis.chutney.scenario.domain.gwt.GwtScenario;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.security.infra.SpringUserService;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.execution.ExecutionRequest;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngine;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngineAsync;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ScenarioExecutionUiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioExecutionUiController.class);
    private static final GwtScenarioMarshaller marshaller = new GwtScenarioMapper();

    private final ScenarioExecutionEngine executionEngine;
    private final ScenarioExecutionEngineAsync executionEngineAsync;
    private final TestCaseRepository testCaseRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper reportObjectMapper;
    private final SpringUserService userService;
    private final DataSetRepository datasetRepository;
    private final ScenarioExecutionReportMapper scenarioExecutionReportMapper;
    private final EmbeddedEnvironmentApi embeddedEnvironmentApi;

    ScenarioExecutionUiController(
        ScenarioExecutionEngine executionEngine,
        ScenarioExecutionEngineAsync executionEngineAsync,
        TestCaseRepository testCaseRepository,
        ObjectMapper objectMapper,
        SpringUserService userService,
        DataSetRepository datasetRepository,
        ScenarioExecutionReportMapper scenarioExecutionReportMapper, EmbeddedEnvironmentApi embeddedEnvironmentApi) {
        this.executionEngine = executionEngine;
        this.executionEngineAsync = executionEngineAsync;
        this.testCaseRepository = testCaseRepository;
        this.objectMapper = objectMapper;
        this.embeddedEnvironmentApi = embeddedEnvironmentApi;
        this.reportObjectMapper = dtoReportObjectMapper();
        this.userService = userService;
        this.datasetRepository = datasetRepository;
        this.scenarioExecutionReportMapper = scenarioExecutionReportMapper;
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = "/api/idea/scenario/execution/{env}")
    public String executeScenarioWitRawContent(@RequestBody IdeaRequest ideaRequest, @PathVariable("env") String env) throws IOException {
        LOGGER.debug("execute Scenario v2 for content='{}'", ideaRequest.content());
        String userId = userService.currentUser().getId();
        GwtScenario gwtScenario = marshaller.deserialize("test title for idea", "test description for idea", ideaRequest.content());

        TestCase testCase = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder()
                .withDescription("test description for idea")
                .withTitle("test title for idea")
                .build())
            .withScenario(gwtScenario)
            .build();

        ScenarioExecutionReport report = executionEngine.simpleSyncExecution(
            new ExecutionRequest(testCase, env, userId)
        );

        return objectMapper.writeValueAsString(report);
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = {"/api/ui/scenario/executionasync/v1/{scenarioId}/{env}", "/api/ui/scenario/executionasync/v1/{scenarioId}/{env}"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String executeScenarioAsyncWithExecutionParameters(@PathVariable("scenarioId") String scenarioId, @PathVariable("env") String env, @RequestBody(required = false) ExecutionDatasetDto dataset) {
        LOGGER.debug("execute async scenario '{}'", scenarioId);
        TestCase testCase = testCaseRepository.findExecutableById(scenarioId).orElseThrow(() -> new ScenarioNotFoundException(scenarioId));
        String userId = userService.currentUser().getId();
        DataSet execDataset = getDataSet(dataset, testCase);
        return executionEngineAsync.execute(new ExecutionRequest(testCase, env, userId, execDataset)).toString();
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = "/api/ui/scenario/executionasync/v1/{scenarioId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String executeScenarioAsyncOnDefaultEnv(@PathVariable("scenarioId") String scenarioId) {
        return executeScenarioAsyncWithExecutionParameters(scenarioId, embeddedEnvironmentApi.defaultEnvironmentName(), null);
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = "/api/ui/scenario/execution/v1/{scenarioId}/{env}")
    public String executeScenario(@PathVariable("scenarioId") String scenarioId, @PathVariable("env") String env) throws IOException {
        LOGGER.debug("executeScenario for scenarioId='{}'", scenarioId);
        TestCase testCase = testCaseRepository.findExecutableById(scenarioId).orElseThrow(() -> new ScenarioNotFoundException(scenarioId));
        String userId = userService.currentUserId();
        DataSet dataset = getDataSetFromTestCase(testCase);
        ScenarioExecutionReport report = executionEngine.simpleSyncExecution(new ExecutionRequest(testCase, env, userId, dataset));

        return reportObjectMapper.writeValueAsString(this.scenarioExecutionReportMapper.toDto(report));
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ')")
    @GetMapping(path = "/api/ui/scenario/executionasync/v1/{scenarioId}/execution/{executionId}")
    public Flux<ServerSentEvent<String>> followScenarioExecution(@PathVariable("scenarioId") String scenarioId, @PathVariable("executionId") Long executionId) {
        LOGGER.debug("followScenarioExecution for scenarioId='{}' and executionID='{}'", scenarioId, executionId);
        return createScenarioExecutionSSEFlux(
            executionEngineAsync.followExecution(scenarioId, executionId)
        );
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = "/api/ui/scenario/executionasync/v1/{scenarioId}/execution/{executionId}/stop")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void stopExecution(@PathVariable("scenarioId") String scenarioId, @PathVariable("executionId") Long executionId) {
        LOGGER.debug("Stop for scenarioId='{}' and executionID='{}'", scenarioId, executionId);
        executionEngineAsync.stop(scenarioId, executionId);
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = "/api/ui/scenario/executionasync/v1/{scenarioId}/execution/{executionId}/pause")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void pauseExecution(@PathVariable("scenarioId") String scenarioId, @PathVariable("executionId") Long executionId) {
        LOGGER.debug("Pause for scenarioId='{}' and executionID='{}'", scenarioId, executionId);
        executionEngineAsync.pause(scenarioId, executionId);
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @PostMapping(path = "/api/ui/scenario/executionasync/v1/{scenarioId}/execution/{executionId}/resume")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void resumeExecution(@PathVariable("scenarioId") String scenarioId, @PathVariable("executionId") Long executionId) {
        LOGGER.debug("Resume for scenarioId='{}' and executionID='{}'", scenarioId, executionId);
        executionEngineAsync.resume(scenarioId, executionId);
    }

    private Flux<ServerSentEvent<String>> createScenarioExecutionSSEFlux(Observable<ScenarioExecutionReport> scenarioExecutionReports) {
        return Flux.from(scenarioExecutionReports.map(
            reportEvent -> ServerSentEvent.<String>builder()
                .id(String.valueOf(reportEvent.executionId))
                .event(reportEvent.report.isTerminated() ? "last" : "partial")
                .data(reportObjectMapper.writeValueAsString(reportEvent))
                .build()
        ).toFlowable(BackpressureStrategy.BUFFER));
    }

    private DataSet getDataSetFromTestCase(TestCase testCase) {
        String defaultDatasetId = testCase.metadata().defaultDataset();
        if (!defaultDatasetId.isEmpty()) {
            return datasetRepository.findById(defaultDatasetId);
        } else {
            return DataSet.NO_DATASET;
        }
    }

    private DataSet getDataSet(ExecutionDatasetDto dataset, TestCase testCase) {
        return Optional.ofNullable(
            fromExecutionDatasetDto(dataset, datasetRepository::findById)
        ).orElseGet(() -> this.getDataSetFromTestCase(testCase));
    }

    // TODO - Use Spring serialization
    public ObjectMapper dtoReportObjectMapper() {
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .findAndRegisterModules();
    }
}
