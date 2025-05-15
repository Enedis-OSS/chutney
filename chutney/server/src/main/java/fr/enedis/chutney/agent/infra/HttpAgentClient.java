/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra;

import static fr.enedis.chutney.agent.api.NodeNetworkController.EXPLORE_URL;
import static fr.enedis.chutney.agent.api.NodeNetworkController.WRAP_UP_URL;

import fr.enedis.chutney.agent.api.dto.ExploreResultApiDto;
import fr.enedis.chutney.agent.api.dto.ExploreResultApiDto.AgentLinkEntity;
import fr.enedis.chutney.agent.api.dto.NetworkConfigurationApiDto;
import fr.enedis.chutney.agent.api.dto.NetworkDescriptionApiDto;
import fr.enedis.chutney.agent.api.mapper.AgentGraphApiMapper;
import fr.enedis.chutney.agent.api.mapper.AgentInfoApiMapper;
import fr.enedis.chutney.agent.api.mapper.EnvironmentApiMapper;
import fr.enedis.chutney.agent.api.mapper.ExploreResultApiMapper;
import fr.enedis.chutney.agent.api.mapper.NetworkConfigurationApiMapper;
import fr.enedis.chutney.agent.api.mapper.NetworkDescriptionApiMapper;
import fr.enedis.chutney.agent.domain.AgentClient;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.agent.domain.explore.ExploreResult;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.engine.domain.delegation.ConnectionChecker;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
class HttpAgentClient implements AgentClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAgentClient.class);

    private final RestTemplate restTemplate;
    private final ConnectionChecker connectionChecker;

    private final NetworkConfigurationApiMapper networkConfigurationApiMapper = new NetworkConfigurationApiMapper(new AgentInfoApiMapper(), new EnvironmentApiMapper());
    private final NetworkDescriptionApiMapper networkDescriptionApiMapper = new NetworkDescriptionApiMapper(
        new NetworkConfigurationApiMapper(new AgentInfoApiMapper(), new EnvironmentApiMapper()),
        new AgentGraphApiMapper(new AgentInfoApiMapper()));
    private final ExploreResultApiMapper exploreResultApiMapper = new ExploreResultApiMapper();

    /**
     * @param connectionChecker used to rapidly fail if the connection to an agent can't be established.<br>
     *                          This is useful as {@link #explore} may take a while before returning.<br>
     *                          So timeout of the supplied {@link RestTemplate} used internally must be kept to a high value whereas the given {@link ConnectionChecker} must have a minimal timeout.
     */
    HttpAgentClient(RestTemplate restTemplate, ConnectionChecker connectionChecker) throws UnknownHostException {
        this.restTemplate = restTemplate;
        this.connectionChecker = connectionChecker;
    }

    /**
     * May take a while before returning, and so timeout here must be kept to a high value.
     *
     * @see AgentClient#explore
     */
    @Override
    public ExploreResult explore(String localName, NamedHostAndPort agentInfo, NetworkConfiguration networkConfiguration) {
        if (!connectionChecker.canConnectTo(agentInfo)) return ExploreResult.EMPTY;
        return exploreByHttp(localName, agentInfo, networkConfiguration);
    }

    @Override
    public void wrapUp(NamedHostAndPort agentInfo, NetworkDescription networkDescription) {
        if (connectionChecker.canConnectTo(agentInfo)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NetworkDescriptionApiDto> request = new HttpEntity<>(networkDescriptionApiMapper.toDto(networkDescription), headers);
            restTemplate.postForObject("https://" + agentInfo.host() + ":" + agentInfo.port() + WRAP_UP_URL, request, Void.class);
        }
    }

    private ExploreResult exploreByHttp(String localName, NamedHostAndPort agentInfo, NetworkConfiguration networkConfiguration) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NetworkConfigurationApiDto> request = new HttpEntity<>(networkConfigurationApiMapper.toDto(networkConfiguration), headers);
            ExploreResultApiDto response = restTemplate.postForObject("https://" + agentInfo.host() + ":" + agentInfo.port() + EXPLORE_URL, request, ExploreResultApiDto.class);
            return exploreResultApiMapper.fromDto(response, new AgentLinkEntity(localName, agentInfo.name()));
        } catch (RestClientException e) {
            LOGGER.warn("Unable to propagate configure to reachable agent : " + agentInfo + " (" + e.getMessage() + ")");
            return ExploreResult.EMPTY;
        }
    }
}
