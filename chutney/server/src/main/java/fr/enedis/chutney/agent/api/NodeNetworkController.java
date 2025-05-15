/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api;

import fr.enedis.chutney.agent.api.dto.ExploreResultApiDto;
import fr.enedis.chutney.agent.api.dto.NetworkConfigurationApiDto;
import fr.enedis.chutney.agent.api.dto.NetworkDescriptionApiDto;
import fr.enedis.chutney.agent.api.mapper.ExploreResultApiMapper;
import fr.enedis.chutney.agent.api.mapper.NetworkConfigurationApiMapper;
import fr.enedis.chutney.agent.api.mapper.NetworkDescriptionApiMapper;
import fr.enedis.chutney.agent.domain.configure.ConfigureService;
import fr.enedis.chutney.agent.domain.configure.GetCurrentNetworkDescriptionService;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.agent.domain.explore.ExploreAgentsService;
import fr.enedis.chutney.agent.domain.explore.ExploreResult;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NodeNetworkController {
    private final ConfigureService configureService;
    private final GetCurrentNetworkDescriptionService getCurrentNetworkDescription;
    private final ExploreAgentsService exploreAgentsService;
    private final EnvironmentApi embeddedEnvironmentApi;

    private final NetworkConfigurationApiMapper networkConfigurationApiMapper;
    private final NetworkDescriptionApiMapper networkDescriptionApiMapper;
    private final ExploreResultApiMapper exploreResultApiMapper;

    public NodeNetworkController(ConfigureService configureService,
                                 GetCurrentNetworkDescriptionService getCurrentNetworkDescription,
                                 ExploreAgentsService exploreAgentsService,
                                 EmbeddedEnvironmentApi embeddedEnvironmentApi,
                                 NetworkDescriptionApiMapper networkDescriptionApiMapper,
                                 ExploreResultApiMapper exploreResultApiMapper,
                                 NetworkConfigurationApiMapper networkConfigurationApiMapper) {
        this.configureService = configureService;
        this.getCurrentNetworkDescription = getCurrentNetworkDescription;
        this.exploreAgentsService = exploreAgentsService;
        this.embeddedEnvironmentApi = embeddedEnvironmentApi;
        this.networkDescriptionApiMapper = networkDescriptionApiMapper;
        this.exploreResultApiMapper = exploreResultApiMapper;
        this.networkConfigurationApiMapper = networkConfigurationApiMapper;
    }

    public static final String CONFIGURE_URL = "/api/v1/agentnetwork/configuration";

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @PostMapping(CONFIGURE_URL)
    public NetworkDescriptionApiDto configure(@RequestBody NetworkConfigurationApiDto networkConfigurationApi) {
        NetworkConfiguration networkConfiguration = networkConfigurationApiMapper.fromDtoAtNow(networkConfigurationApi);
        NetworkConfiguration enhancedWithEnvironmentNetworkConfiguration = networkConfigurationApiMapper.enhanceWithEnvironment(networkConfiguration, embeddedEnvironmentApi.listEnvironments());
        NetworkDescription networkDescription = configureService.configure(enhancedWithEnvironmentNetworkConfiguration);
        return networkDescriptionApiMapper.toDto(networkDescription);
    }

    public static final String DESCRIPTION_URL = "/api/v1/description";

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @GetMapping(DESCRIPTION_URL)
    public NetworkDescriptionApiDto getConfiguration() {
        return networkDescriptionApiMapper.toDto(getCurrentNetworkDescription.getCurrentNetworkDescription());
    }

    public static final String EXPLORE_URL = "/api/v1/agentnetwork/explore";

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @PostMapping(EXPLORE_URL)
    public ExploreResultApiDto explore(@RequestBody NetworkConfigurationApiDto networkConfigurationApiDto) {
        NetworkConfiguration networkConfiguration = networkConfigurationApiMapper.fromDto(networkConfigurationApiDto);
        ExploreResult links = exploreAgentsService.explore(networkConfiguration);
        return exploreResultApiMapper.from(links);
    }

    public static final String WRAP_UP_URL = "/api/v1/agentnetwork/wrapup";

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @PostMapping(WRAP_UP_URL)
    public void wrapUp(@RequestBody NetworkDescriptionApiDto networkDescriptionApiDto) {
        NetworkDescription networkDescription = networkDescriptionApiMapper.fromDto(networkDescriptionApiDto);
        configureService.wrapUpConfiguration(networkDescription);
    }
}
