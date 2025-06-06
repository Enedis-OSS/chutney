/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.step;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.engine.domain.execution.report.Status;
import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

/**
 * Mutable part of a Step, holding data evolving during Scenario execution such as {@link Status}.
 */
public class StepState {

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    private Status status = Status.NOT_EXECUTED;
    private Instant startDate;
    private final List<String> errors = new ArrayList<>();
    private final List<String> informations = new ArrayList<>();
    private String name;
    private Boolean isForStrategyApplied = false;

    public StepState(String name) {
        this.name = name;
    }

    public StepState() {
    }

    void beginExecution() {
        if (!stopwatch.isRunning()) {
            stopwatch.start();
            if (isNull(startDate)) {
                startDate = Instant.now();
            }
            status = Status.RUNNING;
        }
    }

    void endExecution(boolean isParentStep) {
        if (stopwatch.isRunning()) {
            stopwatch.stop();
            if (isParentStep) {
                status = Status.EXECUTED;
            }
        }
    }

    void stopExecution() {
        status = Status.STOPPED;
    }

    void pauseExecution() {
        status = Status.PAUSED;
    }

    void resumeExecution() {
        status = Status.RUNNING;
    }

    void errorOccurred(String... message) {
        status = Status.FAILURE;
        errors.addAll(newArrayList(message));
    }

    void successOccurred(String... message) {
        status = Status.SUCCESS;
        informations.addAll(newArrayList(message));
    }

    void reset() {
        status = Status.NOT_EXECUTED;
        informations.clear();
        errors.clear();
    }

    void startWatch() {
        if (!stopwatch.isRunning()) {
            stopwatch.start();
        }
    }

    void stopWatch() {
        if (stopwatch.isRunning()) {
            stopwatch.stop();
        }
    }

    void addInformation(String... message) {
        informations.addAll(newArrayList(message));
    }

    void addErrors(String... message) {
        errors.addAll(newArrayList(message));
    }

    public Duration duration() {
        return Duration.of(stopwatch.elapsed(TimeUnit.MICROSECONDS), ChronoUnit.MICROS);
    }

    public Status status() {
        return status;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> errors() {
        return unmodifiableList(filterNullAndEmptyMessage(errors));
    }

    public List<String> informations() {
        return unmodifiableList(filterNullAndEmptyMessage(informations));
    }

    public Instant startDate() {
        return ofNullable(startDate).orElse(Instant.now());
    }

    private List<String> filterNullAndEmptyMessage(List<String> messages) {
        return newArrayList(messages).stream().filter(StringUtils::isNotEmpty).toList();
    }

    public Boolean isForStrategyApplied() {
        return isForStrategyApplied;
    }

    public void setIsForStrategyApplied(Boolean isForStrategyApplied) {
        this.isForStrategyApplied = isForStrategyApplied;
    }
}
