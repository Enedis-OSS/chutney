/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.config;

import static fr.enedis.chutney.config.ServerConfigurationValues.CAMPAIGNS_EXECUTOR_POOL_SIZE_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.ENGINE_DELEGATION_PASSWORD_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.ENGINE_DELEGATION_USER_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.ENGINE_EXECUTOR_POOL_SIZE_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.ENGINE_REPORTER_PUBLISHER_TTL_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.EXECUTION_ASYNC_PUBLISHER_DEBOUNCE_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.EXECUTION_ASYNC_PUBLISHER_TTL_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.TASK_SQL_MINIMUM_MEMORY_PERCENTAGE_REQUIRED;
import static fr.enedis.chutney.config.ServerConfigurationValues.TASK_SQL_MINIMUM_MEMORY_PERCENTAGE_REQUIRED_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.TASK_SQL_NB_LOGGED_ROW;
import static fr.enedis.chutney.config.ServerConfigurationValues.TASK_SQL_NB_LOGGED_ROW_SPRING_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enedis.chutney.ExecutionConfiguration;
import fr.enedis.chutney.action.api.EmbeddedActionEngine;
import fr.enedis.chutney.campaign.domain.CampaignEnvironmentUpdateHandler;
import fr.enedis.chutney.campaign.domain.CampaignExecutionRepository;
import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.campaign.domain.CampaignService;
import fr.enedis.chutney.dataset.domain.DataSetRepository;
import fr.enedis.chutney.design.domain.editionlock.TestCaseEditions;
import fr.enedis.chutney.design.domain.editionlock.TestCaseEditionsService;
import fr.enedis.chutney.engine.api.execution.TestEngine;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionEngine;
import fr.enedis.chutney.execution.infra.execution.ExecutionRequestMapper;
import fr.enedis.chutney.execution.infra.execution.ServerTestEngineJavaImpl;
import fr.enedis.chutney.jira.api.JiraXrayEmbeddedApi;
import fr.enedis.chutney.scenario.infra.TestCaseRepositoryAggregator;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngine;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngineAsync;
import fr.enedis.chutney.server.core.domain.execution.ServerTestEngine;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.execution.state.ExecutionStateRepository;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutionsConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionsConfiguration.class);


    /**
     * For fr.enedis.chutney.config.ServerConfiguration#executionConfiguration()
     */
    @Bean
    public ThreadPoolTaskExecutor engineExecutor(@Value(ENGINE_EXECUTOR_POOL_SIZE_SPRING_VALUE) Integer threadForEngine) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadForEngine);
        executor.setMaxPoolSize(threadForEngine);
        executor.setThreadNamePrefix("engine-executor");
        executor.initialize();
        LOGGER.debug("Pool for engine created with size {}", threadForEngine);
        return executor;
    }

    /**
     * For fr.enedis.chutney.config.ServerConfiguration#campaignExecutionEngine()
     */
    @Bean
    public TaskExecutor campaignExecutor(@Value(CAMPAIGNS_EXECUTOR_POOL_SIZE_SPRING_VALUE) Integer threadForCampaigns) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadForCampaigns);
        executor.setMaxPoolSize(threadForCampaigns);
        executor.setThreadNamePrefix("campaign-executor");
        executor.initialize();
        LOGGER.debug("Pool for campaigns created with size {}", threadForCampaigns);
        return executor;
    }

    @Bean
    public ExecutionConfiguration executionConfiguration(
        @Value(ENGINE_REPORTER_PUBLISHER_TTL_SPRING_VALUE) Long reporterTTL,
        @Qualifier("engineExecutor") ThreadPoolTaskExecutor engineExecutor,
        @Value(TASK_SQL_NB_LOGGED_ROW_SPRING_VALUE) String nbLoggedRow,
        @Value(TASK_SQL_MINIMUM_MEMORY_PERCENTAGE_REQUIRED_SPRING_VALUE) String minimumMemoryPercentageRequired,
        @Value(ENGINE_DELEGATION_USER_SPRING_VALUE) String delegateUser,
        @Value(ENGINE_DELEGATION_PASSWORD_SPRING_VALUE) String delegatePassword
    ) {
        Map<String, String> actionsConfiguration = new HashMap<>();
        actionsConfiguration.put(TASK_SQL_NB_LOGGED_ROW, nbLoggedRow);
        actionsConfiguration.put(TASK_SQL_MINIMUM_MEMORY_PERCENTAGE_REQUIRED, minimumMemoryPercentageRequired);
        return new ExecutionConfiguration(reporterTTL, engineExecutor.getThreadPoolExecutor(), actionsConfiguration, delegateUser, delegatePassword);
    }

    @Bean
    ScenarioExecutionEngine scenarioExecutionEngine(ServerTestEngine executionEngine,
                                                    ScenarioExecutionEngineAsync executionEngineAsync) {
        return new ScenarioExecutionEngine(
            executionEngine,
            executionEngineAsync);
    }

    @Bean
    ScenarioExecutionEngineAsync scenarioExecutionEngineAsync(ExecutionHistoryRepository executionHistoryRepository,
                                                              ServerTestEngine executionEngine,
                                                              ExecutionStateRepository executionStateRepository,
                                                              ChutneyMetrics metrics,
                                                              @Qualifier("reportObjectMapper") ObjectMapper objectMapper,
                                                              @Value(EXECUTION_ASYNC_PUBLISHER_TTL_SPRING_VALUE) long replayerRetention,
                                                              @Value(EXECUTION_ASYNC_PUBLISHER_DEBOUNCE_SPRING_VALUE) long debounceMilliSeconds) {
        return new ScenarioExecutionEngineAsync(
            executionHistoryRepository,
            executionEngine,
            executionStateRepository,
            metrics,
            objectMapper,
            replayerRetention,
            debounceMilliSeconds);
    }

    @Bean
    CampaignExecutionEngine campaignExecutionEngine(CampaignRepository campaignRepository,
                                                    CampaignExecutionRepository campaignExecutionRepository,
                                                    ScenarioExecutionEngine scenarioExecutionEngine,
                                                    ScenarioExecutionEngineAsync scenarioExecutionEngineAsync,
                                                    ExecutionHistoryRepository executionHistoryRepository,
                                                    TestCaseRepositoryAggregator testCaseRepository,
                                                    JiraXrayEmbeddedApi jiraXrayEmbeddedApi,
                                                    ChutneyMetrics metrics,
                                                    @Qualifier("campaignExecutor") TaskExecutor campaignExecutor,
                                                    DataSetRepository datasetRepository,
                                                    ObjectMapper objectMapper) { // TODO - Choose explicitly which mapper to use
        return new CampaignExecutionEngine(
            campaignRepository,
            campaignExecutionRepository,
            scenarioExecutionEngine,
            scenarioExecutionEngineAsync,
            executionHistoryRepository,
            testCaseRepository,
            jiraXrayEmbeddedApi,
            metrics,
            new ExecutorServiceAdapter(campaignExecutor),
            datasetRepository,
            objectMapper
        );
    }



    @Bean
    TestCaseEditionsService testCaseEditionsService(TestCaseEditions testCaseEditions, TestCaseRepositoryAggregator testCaseRepository) {
        return new TestCaseEditionsService(testCaseEditions, testCaseRepository);
    }

    @Bean
    TestEngine embeddedTestEngine(fr.enedis.chutney.ExecutionConfiguration executionConfiguration) {
        return executionConfiguration.embeddedTestEngine();
    }

    @Bean
    ServerTestEngine javaTestEngine(TestEngine embeddedTestEngine, ExecutionRequestMapper executionRequestMapper) {
        return new ServerTestEngineJavaImpl(embeddedTestEngine, executionRequestMapper);
    }

    @Bean
    EmbeddedActionEngine embeddedActionEngine(fr.enedis.chutney.ExecutionConfiguration executionConfiguration) {
        return new EmbeddedActionEngine(executionConfiguration.actionTemplateRegistry());
    }


    @Bean
    CampaignService campaignService(CampaignExecutionRepository campaignExecutionRepository) {
        return new CampaignService(campaignExecutionRepository);
    }

    @Bean
    CampaignEnvironmentUpdateHandler campaignEnvironmentUpdateHandler(CampaignRepository campaignRepository) {
        return new CampaignEnvironmentUpdateHandler(campaignRepository);
    }
}
