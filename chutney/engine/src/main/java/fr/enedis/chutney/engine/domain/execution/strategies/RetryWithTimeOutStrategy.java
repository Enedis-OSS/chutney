/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.strategies;

import fr.enedis.chutney.action.spi.time.Duration;
import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContext;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContextImpl;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Retry every retryDelay execution of a Step until success or until timeOut
 * Expects following strategy properties:
 * - timeOut: duration before giving up on the execution of a failed step
 * - retryDelay: waiting duration before retrying execution of a failed step
 * <p>
 * Expected duration format: "floating_positive_number [duration_unit]" where
 * floating_positive_number : the duration value (ex.: 10)
 * time_unit : the duration unit. Valid values are:
 * - "min" or "m" for minutes
 * - "sec" or "s" for seconds
 * - "ms" for milliseconds
 * empty values are interpreted as seconds
 * Example: timeOut: "5 min", or "300 sec", ...
 */

public class RetryWithTimeOutStrategy implements StepExecutionStrategy {

    private static final String TYPE = "retry-with-timeout";

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * @throws IllegalStateException when sleep is interrupted
     */
    @Override
    public Status execute(ScenarioExecution scenarioExecution,
                          Step step,
                          ScenarioContext scenarioContext,
                          Map<String, Object> localContext,
                          StepExecutionStrategies strategies) throws IllegalStateException {

        if (step.strategy().isEmpty()) {
            throw new IllegalArgumentException("Should not have strategy definition empty for retry strategy");
        }
        StepStrategyDefinition strategyDefinition = step.strategy().get();

        // TODO - add a backoff parameter
        String timeOut = strategyDefinition.strategyProperties.getProperty("timeOut", String.class);
        String retryDelay = strategyDefinition.strategyProperties.getProperty("retryDelay", String.class); // TODO - respect State of the art : provide number of retries policy instead
        if (timeOut == null) {
            throw new IllegalStateException("Undefined parameter 'timeOut'"); // TODO - Provide a default value instead
        }
        if (retryDelay == null) {
            throw new IllegalStateException("Undefined parameter 'retryDelay'"); // TODO - Provide a default value instead
        }

        ScenarioContextImpl mergedContext = new ScenarioContextImpl();
        mergedContext.putAll(scenarioContext);
        mergedContext.putAll(localContext);

        String evaluatedRetryDelay = step.dataEvaluator().evaluateString(retryDelay, mergedContext);
        String evaluatedTimeOut = step.dataEvaluator().evaluateString(timeOut, mergedContext);
        long retryDelayMs = toMilliSeconds(evaluatedRetryDelay);
        long timeLeft = toMilliSeconds(evaluatedTimeOut);
        Status st;
        int tries = 1;
        List<String> lastErrors = new ArrayList<>();
        do {
            long tryStartTime = System.currentTimeMillis();
            step.addInformation("Retry strategy definition : [timeOut " + evaluatedTimeOut + "] [delay " + evaluatedRetryDelay + "]");
            step.addInformation("Try number : " + (tries++));

            st = executeAll(scenarioExecution, step, scenarioContext, localContext, strategies);
            if (st == Status.FAILURE) {
                try {
                    step.startWatch();
                    TimeUnit.MILLISECONDS.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Sleeping between executions have been interrupted", e);
                } finally {
                    step.stopWatch();
                }
                timeLeft -= System.currentTimeMillis() - tryStartTime;
            } else {
                if (!lastErrors.isEmpty()) {
                    step.addErrorMessage("Error(s) on last step execution:");
                    lastErrors.forEach(step::addErrorMessage);
                }
                break;
            }

            if (timeLeft > 0) {
                lastErrors.clear();
                lastErrors.addAll(step.errors());
                step.resetExecution();
            }
        } while (timeLeft > 0); // TODO - needs to backoff a bit before retry
        return st;
    }

    private long toMilliSeconds(String duration) {
        double durationInMS = Duration.parse(duration).toMilliseconds();
        return Math.round(durationInMS);
    }

    private Status executeAll(ScenarioExecution scenarioExecution,
                              Step step,
                              ScenarioContext scenarioContext,
                              Map<String, Object> localContext,
                              StepExecutionStrategies strategies) {
        Status st = DefaultStepExecutionStrategy.instance.execute(scenarioExecution, step, scenarioContext, localContext, strategies); // TODO - how do you cancel a try ? I call this a spam strategy, not a retry one !
        if (st == Status.FAILURE) {
            if (scenarioExecution.hasToStop()) {
                step.stopExecution(scenarioExecution);
                return Status.STOPPED;
            }
            return st;
        }
        return Status.SUCCESS;
    }
}

/*
 * Resources
 *
 * Generic:
 * - http://www.baeldung.com/java-completablefuture
 * - http://www.deadcoderising.com/java8-writing-asynchronous-code-with-completablefuture/
 *
 * Specific:
 * - https://crondev.wordpress.com/2017/01/23/timeouts-with-java-8-completablefuture-youre-probably-doing-it-wrong/
 * - http://www.nurkiewicz.com/2015/03/completablefuture-cant-be-interrupted.html
 * - https://stackoverflow.com/questions/11751329/java-execute-action-with-a-number-of-retries-and-a-timeout/
 *
 * Implementation:
 * - https://github.com/jhalterman/failsafe
 * - https://github.com/spring-projects/spring-retry
 * - https://github.com/elennick/retry4j
 * - https://github.com/vsilaev/tascalate-concurrent
 * */
