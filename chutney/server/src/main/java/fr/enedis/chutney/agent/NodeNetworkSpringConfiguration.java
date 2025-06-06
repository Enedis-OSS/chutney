/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent;

import static fr.enedis.chutney.ServerConfigurationValues.CONFIGURATION_FOLDER_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.LOCAL_AGENT_DEFAULT_HOSTNAME_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.LOCAL_AGENT_DEFAULT_NAME_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.SERVER_PORT_SPRING_VALUE;

import fr.enedis.chutney.agent.domain.AgentClient;
import fr.enedis.chutney.agent.domain.configure.ConfigureService;
import fr.enedis.chutney.agent.domain.configure.Explorations;
import fr.enedis.chutney.agent.domain.configure.GetCurrentNetworkDescriptionService;
import fr.enedis.chutney.agent.domain.configure.LocalServerIdentifier;
import fr.enedis.chutney.agent.domain.explore.CurrentNetworkDescription;
import fr.enedis.chutney.agent.domain.explore.ExploreAgentsService;
import fr.enedis.chutney.agent.infra.storage.JsonFileAgentNetworkDao;
import fr.enedis.chutney.engine.domain.delegation.ConnectionChecker;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NodeNetworkSpringConfiguration {

    private static final String NODE_NETWORK_QUALIFIER = "agentnetwork";

    @Bean
    public ObjectMapper agentNetworkObjectMapper() {
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .findAndRegisterModules();
    }

    @Bean
    LocalServerIdentifier localServerIdentifier(@Value(SERVER_PORT_SPRING_VALUE) int port,
                                                @Value(LOCAL_AGENT_DEFAULT_NAME_SPRING_VALUE) Optional<String> defaultLocalName,
                                                @Value(LOCAL_AGENT_DEFAULT_HOSTNAME_SPRING_VALUE) Optional<String> defaultLocalHostName
    ) throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        return new LocalServerIdentifier(
            port,
            defaultLocalName.orElse(localHost.getHostName()),
            defaultLocalHostName.orElse(localHost.getCanonicalHostName()));
    }

    @Bean
    @Qualifier(NODE_NETWORK_QUALIFIER)
    public RestTemplate restTemplateForHttpNodeNetwork(@Qualifier("agentNetworkObjectMapper") ObjectMapper nodeNetworkObjectMapper) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(Lists.newArrayList(new MappingJackson2HttpMessageConverter(nodeNetworkObjectMapper)));
        return restTemplate;
    }

    @Bean
    ExploreAgentsService agentNetwork(Explorations explorations,
                                      AgentClient agentClient,
                                      ConnectionChecker connectionChecker,
                                      LocalServerIdentifier localServerIdentifier) {
        return new ExploreAgentsService(
            explorations,
            agentClient,
            connectionChecker,
            localServerIdentifier);
    }


    @Bean
    ConfigureService configureService(ExploreAgentsService exploreAgentsService,
                                      CurrentNetworkDescription currentNetworkDescription,
                                      LocalServerIdentifier localServerIdentifier,
                                      EmbeddedEnvironmentApi environmentApi) {
        return new ConfigureService(exploreAgentsService, currentNetworkDescription, localServerIdentifier, environmentApi);
    }

    @Bean
    GetCurrentNetworkDescriptionService getCurrentNetworkDescriptionService(CurrentNetworkDescription currentNetworkDescription) {
        return new GetCurrentNetworkDescriptionService(currentNetworkDescription);
    }

    @Bean
    JsonFileAgentNetworkDao getJsonFileAgentNetworkDao(@Value(CONFIGURATION_FOLDER_SPRING_VALUE) String storeFolderPath){
        return new JsonFileAgentNetworkDao(storeFolderPath);
    }
}
