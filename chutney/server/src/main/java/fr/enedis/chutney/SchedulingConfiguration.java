/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import static fr.enedis.chutney.ServerConfigurationValues.SCHEDULED_CAMPAIGNS_EXECUTOR_POOL_SIZE_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.SCHEDULED_PURGE_MAX_CAMPAIGN_EXECUTIONS_SPRING_VALUE;
import static fr.enedis.chutney.ServerConfigurationValues.SCHEDULED_PURGE_MAX_SCENARIO_EXECUTIONS_SPRING_VALUE;
import static fr.enedis.chutney.execution.domain.purge.PurgeServiceImpl.ONE_DAY_MILLIS;

import fr.enedis.chutney.campaign.domain.CampaignExecutionRepository;
import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.execution.api.schedule.ScheduleCampaign;
import fr.enedis.chutney.execution.domain.purge.PurgeServiceImpl;
import fr.enedis.chutney.execution.domain.schedule.CampaignScheduler;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.execution.history.PurgeService;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulingConfiguration.class);

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> {
            LOGGER.error("Uncaught exception in async execution", ex);
        };
    }

    @Bean
    public TaskScheduler taskScheduler() {
        var threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(2);
        threadPoolTaskScheduler.setThreadNamePrefix("task-exec");
        return threadPoolTaskScheduler;
    }

    /**
     * Default task executor for @Async (used for SSE for example)
     * With a default  with default configuration: org.springframework.boot.autoconfigure.task.TaskExecutionProperties.Pool
     */
    @Bean
    public TaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        return builder.threadNamePrefix("app-task-exec").build();
    }

    /**
     * @see ScheduleCampaign#executeScheduledCampaign()
     */
    @Bean
    public TaskExecutor scheduleCampaignsExecutor() {
        return new SimpleAsyncTaskExecutor("schedule-campaigns-executor");
    }

    /**
     * @see CampaignScheduler#executeScheduledCampaigns()
     */
    @Bean
    public ExecutorService scheduledCampaignsExecutor(@Value(SCHEDULED_CAMPAIGNS_EXECUTOR_POOL_SIZE_SPRING_VALUE) Integer threadForScheduledCampaigns) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadForScheduledCampaigns);
        executor.setMaxPoolSize(threadForScheduledCampaigns);
        executor.setThreadNamePrefix("scheduled-campaigns-executor");
        executor.initialize();
        LOGGER.debug("Pool for scheduled campaigns created with size {}", threadForScheduledCampaigns);
        return new ExecutorServiceAdapter(executor);
    }

    @Bean
    public PurgeService purgeService(
        TestCaseRepository testCaseRepository,
        ExecutionHistoryRepository executionRepository,
        CampaignRepository campaignRepository,
        CampaignExecutionRepository campaignExecutionRepository,
        @Value(SCHEDULED_PURGE_MAX_SCENARIO_EXECUTIONS_SPRING_VALUE) Integer maxScenarioExecutionsConfig,
        @Value(SCHEDULED_PURGE_MAX_CAMPAIGN_EXECUTIONS_SPRING_VALUE) Integer maxCampaignExecutionsConfig
    ) {
        return new PurgeServiceImpl(
            testCaseRepository,
            executionRepository,
            campaignRepository,
            campaignExecutionRepository,
            maxScenarioExecutionsConfig,
            ONE_DAY_MILLIS,
            maxCampaignExecutionsConfig,
            ONE_DAY_MILLIS
        );
    }
}
