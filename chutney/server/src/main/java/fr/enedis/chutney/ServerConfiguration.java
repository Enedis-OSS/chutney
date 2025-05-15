/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import static fr.enedis.chutney.ServerConfigurationValues.CAMPAIGNS_EXECUTOR_POOL_SIZE_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.ENGINE_DELEGATION_PASSWORD_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.ENGINE_DELEGATION_USER_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.ENGINE_EXECUTOR_POOL_SIZE_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.ENGINE_REPORTER_PUBLISHER_TTL_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.EXECUTION_ASYNC_PUBLISHER_DEBOUNCE_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.EXECUTION_ASYNC_PUBLISHER_TTL_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.SERVER_PORT_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.TASK_SQL_NB_LOGGED_ROW;
import static fr.enedis.chutney.ServerConfigurationValues.TASK_SQL_NB_LOGGED_ROW_SPRING_VALUE;

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
import fr.enedis.chutney.index.infra.LuceneIndexRepository;
import fr.enedis.chutney.index.infra.config.IndexConfig;
import fr.enedis.chutney.index.infra.config.OnDiskIndexConfig;
import fr.enedis.chutney.jira.api.JiraXrayEmbeddedApi;
import fr.enedis.chutney.scenario.infra.TestCaseRepositoryAggregator;
import fr.enedis.chutney.security.domain.AuthenticationService;
import fr.enedis.chutney.security.domain.Authorizations;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngine;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngineAsync;
import fr.enedis.chutney.server.core.domain.execution.ServerTestEngine;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.execution.state.ExecutionStateRepository;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication(exclude = {LiquibaseAutoConfiguration.class, ActiveMQAutoConfiguration.class, MongoAutoConfiguration.class})
@EnableAspectJAutoProxy
public class ServerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfiguration.class);

    @Value(SERVER_PORT_SPRING_VALUE)
    int port;

    @PostConstruct
    public void logPort() throws UnknownHostException {
        LOGGER.debug("Starting server {} on {}", InetAddress.getLocalHost().getCanonicalHostName(), port);
    }

    /**
     * For fr.enedis.chutney.ServerConfiguration#executionConfiguration()
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
     * For fr.enedis.chutney.ServerConfiguration#campaignExecutionEngine()
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
        @Value(ENGINE_DELEGATION_USER_SPRING_VALUE) String delegateUser,
        @Value(ENGINE_DELEGATION_PASSWORD_SPRING_VALUE) String delegatePassword
    ) {
        Map<String, String> actionsConfiguration = new HashMap<>();
        actionsConfiguration.put(TASK_SQL_NB_LOGGED_ROW, nbLoggedRow);
        return new ExecutionConfiguration(reporterTTL, engineExecutor.getThreadPoolExecutor(), actionsConfiguration, delegateUser, delegatePassword);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:changelog/db.changelog-master.xml");
        liquibase.setContexts("!test");
        liquibase.setDataSource(dataSource);
        return liquibase;
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
    TestEngine embeddedTestEngine(ExecutionConfiguration executionConfiguration) {
        return executionConfiguration.embeddedTestEngine();
    }

    @Bean
    ServerTestEngine javaTestEngine(TestEngine embeddedTestEngine, ExecutionRequestMapper executionRequestMapper) {
        return new ServerTestEngineJavaImpl(embeddedTestEngine, executionRequestMapper);
    }

    @Bean
    EmbeddedActionEngine embeddedActionEngine(ExecutionConfiguration executionConfiguration) {
        return new EmbeddedActionEngine(executionConfiguration.actionTemplateRegistry());
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    CampaignService campaignService(CampaignExecutionRepository campaignExecutionRepository) {
        return new CampaignService(campaignExecutionRepository);
    }

    @Bean
    CampaignEnvironmentUpdateHandler campaignEnvironmentUpdateHandler(CampaignRepository campaignRepository) {
        return new CampaignEnvironmentUpdateHandler(campaignRepository);
    }

    @Bean
    public AuthenticationService authenticationService(Authorizations authorizations) {
        return new AuthenticationService(authorizations);
    }

    // TODO - To move in infra when it will not be used in domain (ScenarioExecutionEngineAsync)
    @Bean
    public ObjectMapper reportObjectMapper() {
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .findAndRegisterModules();
    }

    @Bean
    public LuceneIndexRepository reportLuceneIndexRepository(IndexConfig reportIndexConfig) {
        return new LuceneIndexRepository(reportIndexConfig);
    }

    @Bean
    public LuceneIndexRepository scenarioLuceneIndexRepository(IndexConfig scenarioIndexConfig) {
        return new LuceneIndexRepository(scenarioIndexConfig);
    }

    @Bean
    public LuceneIndexRepository datasetLuceneIndexRepository(IndexConfig datasetIndexConfig) {
        return new LuceneIndexRepository(datasetIndexConfig);
    }

    @Bean
    public LuceneIndexRepository campaignLuceneIndexRepository(IndexConfig campaignIndexConfig) {
        return new LuceneIndexRepository(campaignIndexConfig);
    }

    @Bean
    public IndexConfig reportIndexConfig(@Value("${chutney.index-folder:~/.chutney/index}") String directory) {
        return new OnDiskIndexConfig(directory, "report");
    }

    @Bean
    public IndexConfig scenarioIndexConfig(@Value("${chutney.index-folder:~/.chutney/index}") String directory) {
        return new OnDiskIndexConfig(directory, "scenario");
    }

    @Bean
    public IndexConfig datasetIndexConfig(@Value("${chutney.index-folder:~/.chutney/index}") String directory) {
        return new OnDiskIndexConfig(directory, "dataset");
    }

    @Bean
    public IndexConfig campaignIndexConfig(@Value("${chutney.index-folder:~/.chutney/index}") String directory) {
        return new OnDiskIndexConfig(directory, "campaign");
    }

}
