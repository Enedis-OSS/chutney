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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataMigrationExecutor implements CommandLineRunner {

    private final List<DataMigrator> dataMigrators;
    private final ExecutorService executorService;


    public DataMigrationExecutor(List<DataMigrator> dataMigrators) {
        this.dataMigrators = dataMigrators;
        executorService = Executors.newFixedThreadPool(dataMigrators.size());
    }

    @Override
    public void run(String... args) throws Exception {
        dataMigrators.forEach(dataMigrator -> executorService.submit(dataMigrator::migrate));
        executorService.shutdown();
    }
}
