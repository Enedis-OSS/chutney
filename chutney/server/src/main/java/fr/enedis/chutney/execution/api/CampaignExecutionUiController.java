/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api;

import static fr.enedis.chutney.campaign.api.dto.CampaignExecutionReportMapper.toDto;
import static fr.enedis.chutney.dataset.api.DataSetMapper.fromExecutionDatasetDto;

import fr.enedis.chutney.campaign.api.dto.CampaignExecutionReportDto;
import fr.enedis.chutney.campaign.api.dto.CampaignExecutionReportMapper;
import fr.enedis.chutney.dataset.api.ExecutionDatasetDto;
import fr.enedis.chutney.dataset.domain.DataSetRepository;
import fr.enedis.chutney.execution.api.report.surefire.SurefireCampaignExecutionReportBuilder;
import fr.enedis.chutney.execution.api.report.surefire.SurefireScenarioExecutionReportBuilder;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionEngine;
import fr.enedis.chutney.security.infra.SpringUserService;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CampaignExecutionUiController.BASE_URL)
public class CampaignExecutionUiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignExecutionUiController.class);
    static final String BASE_URL = "/api/ui/campaign/execution/v1";

    private final CampaignExecutionEngine campaignExecutionEngine;
    private final SurefireCampaignExecutionReportBuilder surefireCampaignExecutionReportBuilder;
    private final SpringUserService userService;
    private final CampaignExecutionApiMapper campaignExecutionApiMapper;
    private final DataSetRepository datasetRepository;

    public CampaignExecutionUiController(
        CampaignExecutionEngine campaignExecutionEngine,
        SurefireScenarioExecutionReportBuilder surefireScenarioExecutionReportBuilder,
        SpringUserService userService,
        CampaignExecutionApiMapper campaignExecutionApiMapper,
        DataSetRepository datasetRepository) {
        this.campaignExecutionEngine = campaignExecutionEngine;
        this.surefireCampaignExecutionReportBuilder = new SurefireCampaignExecutionReportBuilder(surefireScenarioExecutionReportBuilder);
        this.userService = userService;
        this.campaignExecutionApiMapper = campaignExecutionApiMapper;
        this.datasetRepository = datasetRepository;
    }


    @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
    @GetMapping(path = {"/{campaignName}/lastExecution", "/{campaignName}/{env}/lastExecution"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignExecutionReportSummaryDto getLastCampaignExecution(@PathVariable("campaignId") Long campaignId) {
        CampaignExecution lastCampaignExecution = campaignExecutionEngine.getLastCampaignExecution(campaignId);
        return campaignExecutionApiMapper.toCampaignExecutionReportSummaryDto(lastCampaignExecution);
    }


    @PreAuthorize("hasAuthority('CAMPAIGN_EXECUTE')")
    @GetMapping(path = {"/{campaignName}", "/{campaignName}/{env}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CampaignExecutionReportDto> executeCampaignByName(@PathVariable("campaignName") String campaignName, @PathVariable("env") Optional<String> environment) {
        String userId = userService.currentUser().getId();
        List<CampaignExecution> reports = campaignExecutionEngine.executeByName(campaignName, environment.orElse(null), userId);
        return reports.stream()
            .map(CampaignExecutionReportMapper::toDto)
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_EXECUTE')")
    @PostMapping(path = {"/replay/{campaignExecutionId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignExecutionReportDto replayFailedScenario(@PathVariable("campaignExecutionId") Long campaignExecutionId) {
        String userId = userService.currentUser().getId();
        CampaignExecution newExecution = campaignExecutionEngine.replayFailedScenariosExecutionsForExecution(campaignExecutionId, userId);
        return toDto(newExecution);
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_EXECUTE')")
    @GetMapping(path = {"/{campaignPattern}/surefire", "/{campaignPattern}/surefire/{env}"}, produces = "application/zip")
    public byte[] executeCampaignsByPatternWithSurefireReport(HttpServletResponse response, @PathVariable("campaignPattern") String campaignPattern, @PathVariable("env") Optional<String> environment) {
        String userId = userService.currentUser().getId();
        response.addHeader("Content-Disposition", "attachment; filename=\"surefire-report.zip\"");
        List<CampaignExecution> reports = campaignExecutionEngine.executeByName(campaignPattern, environment.orElse(null), userId);
        return surefireCampaignExecutionReportBuilder.createReport(reports);
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_EXECUTE')")
    @PostMapping(path = "/{executionId}/stop")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void stopExecution(@PathVariable("executionId") Long executionId) {
        LOGGER.debug("Stop campaign execution {}", executionId);
        campaignExecutionEngine.stopExecution(executionId);
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_EXECUTE')")
    @PostMapping(path = {"/byID/{campaignId}", "/byID/{campaignId}/{env}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignExecutionReportDto executeCampaignById(
        @PathVariable("campaignId") Long campaignId,
        @PathVariable("env") Optional<String> environment,
        @RequestBody ExecutionDatasetDto dataset
    ) {
        String userId = userService.currentUser().getId();
        DataSet ds = fromExecutionDatasetDto(dataset, datasetRepository::findById);
        CampaignExecution report = campaignExecutionEngine.executeById(campaignId, environment.orElse(null), ds, userId);
        return toDto(report);
    }
}
