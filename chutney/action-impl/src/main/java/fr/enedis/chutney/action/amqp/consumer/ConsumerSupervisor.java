/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.amqp.consumer;

import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

public class ConsumerSupervisor {

    private static final int LOCK_WAITING = 500;
    private static ConsumerSupervisor instance;
    private final Set<String> queuesLocked = new HashSet<>();

    private ConsumerSupervisor() {
    }

    public static synchronized ConsumerSupervisor getInstance() {
        if (instance == null) {
            instance = new ConsumerSupervisor();
        }
        return instance;
    }

    public boolean isLocked(String queueName) {
        return queuesLocked.contains(queueName);
    }

    public synchronized boolean lock(String queueName) {
        return queuesLocked.add(queueName);
    }

    public synchronized void unlock(String queueName) {
        queuesLocked.remove(queueName);
    }

    public Pair<Boolean, Long> waitUntilQueueAvailable(String queueName, long originalDuration, Logger logger) throws InterruptedException {
        long timeLeft = originalDuration;

        boolean locked = lock(queueName);
        while (!locked && timeLeft >= LOCK_WAITING) {
            timeLeft -= LOCK_WAITING;
            TimeUnit.MILLISECONDS.sleep(LOCK_WAITING);
            locked = lock(queueName);
        }

        if (!locked) {
            logger.error("Cannot consume on queue [" + queueName + "]. Another consumer already listening on this queue");
            return Pair.of(false, 0L);
        } else if (originalDuration - timeLeft > 0) {
            logger.info("Waited " + (originalDuration - timeLeft) + " ms to acquire lock to consume queue " + queueName);
        }
        return Pair.of(true, timeLeft);
    }
}
