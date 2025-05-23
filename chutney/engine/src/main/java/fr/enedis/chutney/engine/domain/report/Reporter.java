/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.report;


import static fr.enedis.chutney.engine.domain.execution.report.Status.PAUSED;
import static fr.enedis.chutney.engine.domain.execution.report.Status.RUNNING;

import fr.enedis.chutney.engine.domain.execution.RxBus;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.event.BeginStepExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.event.EndScenarioExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.event.EndStepExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.event.Event;
import fr.enedis.chutney.engine.domain.execution.event.PauseStepExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.event.StartScenarioExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReportBuilder;
import fr.enedis.chutney.engine.domain.execution.strategies.StepStrategyDefinition;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reporter.class);
    private static final long DEFAULT_RETENTION_DELAY_SECONDS = 5;

    private final Map<Long, Subject<StepExecutionReport>> reportsPublishers = new ConcurrentHashMap<>();
    private final Map<Long, Step> rootSteps = new ConcurrentHashMap<>();
    private long retentionDelaySeconds;

    public Reporter() {
        this(DEFAULT_RETENTION_DELAY_SECONDS);
    }

    public Reporter(long retentionDelaySeconds) {
        this.retentionDelaySeconds = retentionDelaySeconds;
        busRegistration();
    }

    public Observable<StepExecutionReport> subscribeOnExecution(Long executionId) {
        LOGGER.trace("Subscribe for execution {}", executionId);
        return Optional.ofNullable((Observable<StepExecutionReport>) reportsPublishers.get(executionId))
            .orElseGet(Observable::empty);
    }

    public void setRetentionDelaySeconds(long retentionDelaySeconds) {
        this.retentionDelaySeconds = retentionDelaySeconds;
    }

    public void createPublisher(Long executionId, Step rootStep) {
        LOGGER.trace("Create publisher for execution {}", executionId);
        reportsPublishers.put(executionId, ReplaySubject.<StepExecutionReport>createWithSize(1).toSerialized());
        rootSteps.put(executionId, rootStep);
        LOGGER.debug("Publishers map size : {}", reportsPublishers.size());
    }

    private void storeRootStepAndPublishReport(StartScenarioExecutionEvent event) {
        LOGGER.trace("Store root step for execution {}", event.executionId());
        rootSteps.put(event.executionId(), event.step);
        publishReport(event);
    }

    private void publishReport(Event event) {
        LOGGER.trace("Publish report for execution {}", event.executionId());
        doIfPublisherExists(event.executionId(), (observer) -> {
            try {
                observer.onNext(generateRunningReport(event.executionId()));
            } catch (Exception e) {
                LOGGER.warn("Failed to generate report for execution {}", event.executionId(), e);
            }
        });
    }

    private void publishLastReport(Event event) {
        LOGGER.trace("Publish report for execution {}", event.executionId());
        doIfPublisherExists(event.executionId(), (observer) -> {
            try {
                observer.onNext(generateLastReport(event.executionId()));
            } catch (Exception e) {
                LOGGER.warn("Failed to generate report for execution {}", event.executionId(), e);
            }
        });
    }

    private void publishReportAndCompletePublisher(Event event) {
        doIfPublisherExists(event.executionId(), (observer) -> {
            publishLastReport(event);
            completePublisher(event.executionId(), observer);
        });
    }

    private StepExecutionReport generateRunningReport(long executionId) throws CannotGenerateReportException {
        Step step = rootSteps.get(executionId);
        final Status calculatedRootStepStatus = step.status();

        final Status finalStatus;
        if (!calculatedRootStepStatus.equals(RUNNING) && !calculatedRootStepStatus.equals(PAUSED)) {
            finalStatus = RUNNING;
        } else {
            finalStatus = calculatedRootStepStatus;
        }
        return generateReport(step, s -> finalStatus, getEnvironment(step));
    }

    private StepExecutionReport generateLastReport(long executionId) throws CannotGenerateReportException {
        Step step = rootSteps.get(executionId);
        return generateReport(step, Step::status, getEnvironment(step));
    }

    private static String getEnvironment(Step step) {
        if (step.isParentStep()) {
            return getEnvironment(step.subSteps().getFirst());
        }
        return (String) step.getScenarioContext().get("environment");
    }

    StepExecutionReport generateReport(Step step, Function<Step, Status> statusSupplier, String env) throws CannotGenerateReportException {
        if (step == null) {
            throw new CannotGenerateReportException("Cannot generate report: Step is null.");
        }

        List<Step> subStepsCopy = new ArrayList<>(step.subSteps());

        try {
            return new StepExecutionReportBuilder()
                .setName(step.name())
                .setEnvironment(env)
                .setDuration(step.duration().toMillis())
                .setStartDate(step.startDate())
                .setStatus(statusSupplier.apply(step))
                .setInformation(step.informations())
                .setErrors(step.errors())
                .setSteps(subStepsCopy.stream()
                    .map(subStep -> generateReport(subStep, Step::status, env))
                    .collect(Collectors.toList()))
                .setEvaluatedInputs(step.getEvaluatedInputs())
                .setStepResults(step.getStepOutputs())
                .setEvaluatedInputsSnapshot(step.getStepContextInputSnapshot())
                .setStepResultsSnapshot(step.getStepContextOutputSnapshot())
                .setScenarioContext(step.getScenarioContext())
                .setType(step.type())
                .setTarget(step.target())
                .setStrategy(guardNullStrategy(step.strategy()))
                .createStepExecutionReport();
        } catch (CannotGenerateReportException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotGenerateReportException("Unexpected error while generating report for step " + step.name(), e);
        }
    }

    /* TODO mbb - hack - remove me when core module domain is decouple from lite-engine domain & API */
    private String guardNullStrategy(Optional<StepStrategyDefinition> strategy) {
        return strategy.map(stepStrategyDefinition -> stepStrategyDefinition.type).orElse(null);
    }

    private void completePublisher(long executionId, Observer<StepExecutionReport> observer) {
        LOGGER.trace("Complete publisher for execution {}", executionId);
        observer.onComplete();
        if (retentionDelaySeconds > 0) {
            Completable.timer(retentionDelaySeconds, TimeUnit.SECONDS, Schedulers.io())
                .subscribe(
                    () -> {
                        rootSteps.remove(executionId);
                        reportsPublishers.remove(executionId);
                        LOGGER.trace("Remove publisher for execution {}", executionId);
                    },
                    throwable -> LOGGER.error("Cannot remove publisher for execution {}", executionId, throwable)
                );
        } else {
            rootSteps.remove(executionId);
            reportsPublishers.remove(executionId);
            LOGGER.trace("Remove publisher for execution {}", executionId);
        }
    }


    private void doIfPublisherExists(long executionId, Consumer<Observer<StepExecutionReport>> consumer) {
        Optional.ofNullable((Observer<StepExecutionReport>) reportsPublishers.get(executionId))
            .ifPresent(consumer);
    }

    private void busRegistration() {
        RxBus bus = RxBus.getInstance();
        bus.register(StartScenarioExecutionEvent.class, this::storeRootStepAndPublishReport);
        bus.register(BeginStepExecutionEvent.class, this::publishReport);
        bus.register(EndStepExecutionEvent.class, this::publishReport);
        bus.register(PauseStepExecutionEvent.class, this::publishReport);
        bus.register(EndScenarioExecutionEvent.class, this::publishReportAndCompletePublisher);
    }
}
