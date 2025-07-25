/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import static java.util.Collections.emptyMap;

import fr.enedis.chutney.jira.domain.JiraRepository;
import fr.enedis.chutney.jira.domain.JiraServerConfiguration;
import fr.enedis.chutney.jira.domain.JiraXrayService;
import fr.enedis.chutney.jira.xrayapi.XrayTestExecTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(JiraController.BASE_URL)
public class JiraController {

    public static final String BASE_URL = "/api/ui/jira/v1/";
    public static final String BASE_SCENARIO_URL = "scenario";
    public static final String BASE_CAMPAIGN_URL = "campaign";
    public static final String BASE_CAMPAIGN_EXEC_URL = "campaign_execution";
    public static final String BASE_TEST_EXEC_URL = "testexec";
    public static final String BASE_CONFIGURATION_URL = "configuration";

    private final JiraRepository jiraRepository;
    private final JiraXrayService jiraXrayService;

    public JiraController(JiraRepository jiraRepository, JiraXrayService jiraXrayService) {
        this.jiraRepository = jiraRepository;
        this.jiraXrayService = jiraXrayService;
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ') or hasAuthority('CAMPAIGN_WRITE')")
    @GetMapping(path = BASE_SCENARIO_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, JiraScenarioLinksDto> getLinkedScenarios() {
        var linkedScenarios = jiraRepository.getAllLinkedScenarios();
        var linkedScenariosWithDataset = jiraRepository.getAllLinkedScenariosWithDataset();
        var scenarios = new HashSet<String>();
        scenarios.addAll(linkedScenarios.keySet());
        scenarios.addAll(linkedScenariosWithDataset.keySet());
        Map<String, JiraScenarioLinksDto> result = new HashMap<>();
        for (String scenarioId : scenarios) {
            ImmutableJiraScenarioLinksDto jiraScenarioLinksDto = ImmutableJiraScenarioLinksDto.builder()
                .id(linkedScenarios.get(scenarioId))
                .chutneyId(scenarioId)
                .datasetLinks(linkedScenariosWithDataset.getOrDefault(scenarioId, emptyMap()))
                .build();
            result.put(jiraScenarioLinksDto.chutneyId(), jiraScenarioLinksDto);
        }
        return result;
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
    @GetMapping(path = BASE_CAMPAIGN_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getLinkedCampaigns() {
        return jiraRepository.getAllLinkedCampaigns();
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @GetMapping(path = BASE_SCENARIO_URL + "/{scenarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JiraScenarioLinksDto getByScenarioId(@PathVariable String scenarioId) {
        String jiraId = jiraRepository.getByScenarioId(scenarioId);
        Map<String, String> datasetLinks = jiraRepository.getAllLinkedScenariosWithDataset().getOrDefault(scenarioId, new HashMap<>());

        return ImmutableJiraScenarioLinksDto.builder()
            .id(jiraId)
            .chutneyId(scenarioId)
            .datasetLinks(datasetLinks)
            .build();
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @PostMapping(path = BASE_SCENARIO_URL,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public JiraScenarioLinksDto saveForScenario(@RequestBody JiraScenarioLinksDto dto) {
        jiraRepository.saveForScenario(dto.chutneyId(), dto.id());
        jiraRepository.saveDatasetForScenario(dto.chutneyId(), dto.datasetLinks());
        return ImmutableJiraScenarioLinksDto.builder()
            .id(dto.id())
            .chutneyId(dto.chutneyId())
            .datasetLinks(dto.datasetLinks())
            .build();
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @DeleteMapping(path = BASE_SCENARIO_URL + "/{scenarioId}")
    public void removeForScenario(@PathVariable String scenarioId) {
        jiraRepository.removeForScenario(scenarioId);
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
    @GetMapping(path = BASE_CAMPAIGN_URL + "/{campaignId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JiraDto getByCampaignId(@PathVariable String campaignId) {
        String jiraId = jiraRepository.getByCampaignId(campaignId);
        return ImmutableJiraDto.builder()
            .id(jiraId)
            .chutneyId(campaignId)
            .build();
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
    @GetMapping(path = BASE_TEST_EXEC_URL + "/{testExecId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JiraDto> getScenariosByTestExecutionId(@PathVariable String testExecId) {
        if (testExecId.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, String> allLinkedScenarios = jiraRepository.getAllLinkedScenarios();
        Map<String, XrayTestExecTest> collect = jiraXrayService.getTestExecutionScenarios(testExecId).stream().collect(Collectors.toMap(XrayTestExecTest::getKey, Function.identity()));

        return allLinkedScenarios.entrySet()
            .stream()
            .filter(entry -> collect.get(entry.getValue()) != null)
            .map(m -> ImmutableJiraDto.builder()
                .id(m.getValue())
                .chutneyId(m.getKey())
                .executionStatus(Optional.ofNullable(collect.get(m.getValue()).getStatus()))
                .build())
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
    @GetMapping(path = BASE_CAMPAIGN_EXEC_URL + "/{campaignExecutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JiraTestExecutionDto getScenariosByCampaignExecutionId(@PathVariable String campaignExecutionId) {
        if (campaignExecutionId.isEmpty()) {
            throw new IllegalArgumentException("Empty campaign execution id");
        }

        String testExecId = jiraRepository.getByCampaignExecutionId(campaignExecutionId);
        List<JiraDto> jiraDtoList = getScenariosByTestExecutionId(testExecId);

        return ImmutableJiraTestExecutionDto.builder()
            .id(testExecId)
            .jiraScenarios(jiraDtoList)
            .build();
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
    @PostMapping(path = BASE_CAMPAIGN_URL,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public JiraDto saveForCampaign(@RequestBody JiraDto jiraDto) {
        jiraRepository.saveForCampaign(jiraDto.chutneyId(), jiraDto.id());
        return ImmutableJiraDto.builder()
            .id(jiraDto.id())
            .chutneyId(jiraDto.chutneyId())
            .build();
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
    @DeleteMapping(path = BASE_CAMPAIGN_URL + "/{campaignId}")
    public void removeForCampaign(@PathVariable String campaignId) {
        jiraRepository.removeForCampaign(campaignId);
    }

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @GetMapping(path = BASE_CONFIGURATION_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public JiraConfigurationDto getConfiguration() {
        JiraServerConfiguration jiraServerConfiguration = jiraRepository.loadServerConfiguration();
        return ImmutableJiraConfigurationDto.builder()
            .url(jiraServerConfiguration.url())
            .username(jiraServerConfiguration.username())
            .password(jiraServerConfiguration.password())
            .urlProxy(jiraServerConfiguration.urlProxy())
            .userProxy(jiraServerConfiguration.userProxy())
            .passwordProxy(jiraServerConfiguration.passwordProxy())
            .build();
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ') or hasAuthority('CAMPAIGN_READ')")
    @GetMapping(path = BASE_CONFIGURATION_URL + "/url", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getConfigurationUrl() {
        JiraServerConfiguration jiraServerConfiguration = jiraRepository.loadServerConfiguration();
        return jiraServerConfiguration.url();
    }

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @PostMapping(path = BASE_CONFIGURATION_URL,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public void saveConfiguration(@RequestBody JiraConfigurationDto jiraConfigurationDto) {
        jiraRepository.saveServerConfiguration(
            new JiraServerConfiguration(
                jiraConfigurationDto.url(),
                jiraConfigurationDto.username(),
                jiraConfigurationDto.password(),
                jiraConfigurationDto.urlProxy().orElse(null),
                jiraConfigurationDto.userProxy().orElse(null),
                jiraConfigurationDto.passwordProxy().orElse(null)
            )
        );
    }

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @DeleteMapping(path = BASE_CONFIGURATION_URL)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void cleanConfiguration() {
        jiraRepository.cleanServerConfiguration();
    }

    @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
    @PutMapping(path = BASE_TEST_EXEC_URL + "/{testExecId}",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateScenarioStatus(@PathVariable String testExecId, @RequestBody JiraDto jiraDto) {
        if (!testExecId.isEmpty() && jiraDto.executionStatus().isPresent()) {
            jiraXrayService.updateScenarioStatus(testExecId, jiraDto.chutneyId(), jiraDto.executionStatus().get());
        }
    }
}
