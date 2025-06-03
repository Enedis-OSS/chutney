/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.migration.domain;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataMigrationExecutor implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataMigrationExecutor.class);

    private final List<DataMigrator> dataMigrators;
    private final ExecutorService executorService;


    public DataMigrationExecutor(List<DataMigrator> dataMigrators) {
        this.dataMigrators = dataMigrators;
        executorService = Executors.newFixedThreadPool(dataMigrators.size());
    }

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("Start data migration");
        dataMigrators.forEach(dataMigrator -> executorService.submit(dataMigrator::migrate));
        awaitTerminationAfterShutdown(executorService);
        LOGGER.info("End data migration");
    }

    public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(1, TimeUnit.HOURS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
        }
    }
}
