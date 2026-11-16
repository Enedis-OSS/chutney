/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution;

import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.ActionsConfiguration;
import fr.enedis.chutney.engine.domain.execution.command.PauseExecutionCommand;
import fr.enedis.chutney.engine.domain.execution.command.ResumeExecutionCommand;
import fr.enedis.chutney.engine.domain.execution.command.StopExecutionCommand;
import fr.enedis.chutney.engine.domain.execution.event.EndScenarioExecutionEvent;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ScenarioExecution {

    private final List<FinallyAction> finallyActions = new ArrayList<>();
    private final ActionsConfiguration actionConfiguration;
    public final long executionId;

    private volatile boolean pause = false;
    private volatile boolean stop = false;
    private volatile boolean forcedStop = false;
    private Thread interruptibleThread;

    private Disposable endExecutionSubscriber;

    public static ScenarioExecution createScenarioExecution(ActionsConfiguration actionConfiguration) {
        long executionId = UUID.randomUUID().getMostSignificantBits();
        return new ScenarioExecution(executionId, actionConfiguration);
    }

    private ScenarioExecution(long executionId, ActionsConfiguration actionConfiguration) {
        this.executionId = executionId;
        this.actionConfiguration = actionConfiguration;

        final Disposable pauseSubscriber = RxBus.getInstance()
            .registerOnExecutionId(PauseExecutionCommand.class, executionId, e -> this.pause());
        final Disposable stopSubscriber = RxBus.getInstance()
            .registerOnExecutionId(StopExecutionCommand.class, executionId, e -> this.stop(((StopExecutionCommand) e).force));
        final Disposable resumeSubscriber = RxBus.getInstance()
            .registerOnExecutionId(ResumeExecutionCommand.class, executionId, e -> this.resume());

        endExecutionSubscriber = RxBus.getInstance().registerOnExecutionId(EndScenarioExecutionEvent.class, executionId, e -> {
            pauseSubscriber.dispose();
            stopSubscriber.dispose();
            resumeSubscriber.dispose();
            endExecutionSubscriber.dispose();
        });
    }

    public void registerFinallyAction(FinallyAction finallyAction) {
        finallyActions.add(finallyAction);
    }

    public void initFinallyActionExecution() {
        this.forcedStop = false;
        this.stop = false; // In case of a stopped scenario, we should set it to false in order to execute finally actions
    }

    public void waitForRestart() {
        while (pause && !stop) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                if (!stop) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * From now on, a forced stop request interrupts the calling thread.
     */
    public synchronized void allowInterruption() {
        interruptibleThread = Thread.currentThread();
    }

    /**
     * To call from the thread which allowed interruption.
     */
    public synchronized void forbidInterruption() {
        interruptibleThread = null;
        Thread.interrupted();
    }

    public boolean hasToPause() {
        return pause;
    }

    public boolean hasToStop() {
        return stop;
    }

    public boolean hasToStopImmediately() {
        return forcedStop;
    }

    private void pause() {
        pause = true;
    }

    private synchronized void stop(boolean force) {
        stop = true;
        forcedStop = forcedStop || force;
        if (force && interruptibleThread != null) {
            interruptibleThread.interrupt();
        }
    }

    private void resume() {
        pause = false;
    }

    public ActionsConfiguration getActionsConfiguration() {
        return actionConfiguration;
    }

    public List<FinallyAction> finallyActions() {
        return finallyActions;
    }
}
