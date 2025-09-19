/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.config;

import static fr.enedis.chutney.config.ServerConfigurationValues.INDEXING_TTL_UNIT_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.INDEXING_TTL_VALUE_SPRING_VALUE;

import fr.enedis.chutney.index.infra.LuceneIndexRepository;
import fr.enedis.chutney.index.infra.config.IndexConfig;
import fr.enedis.chutney.index.infra.config.OnDiskIndexConfig;
import fr.enedis.chutney.migration.domain.DataMigrationExecutor;
import fr.enedis.chutney.migration.domain.DataMigrator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class IndexConfiguration {
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

    @Bean
    public DataMigrationExecutor dataMigrationExecutor(
        @Value(INDEXING_TTL_VALUE_SPRING_VALUE) long indexingTtlValue,
        @Value(INDEXING_TTL_UNIT_SPRING_VALUE) String indexingTtlUnit,
        List<DataMigrator> dataMigrators
    ) {
        return new DataMigrationExecutor(indexingTtlValue, indexingTtlUnit, dataMigrators);
    }
}
