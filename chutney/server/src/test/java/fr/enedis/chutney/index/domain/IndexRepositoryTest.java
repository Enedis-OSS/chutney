/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.domain;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.campaign.infra.index.CampaignIndexRepository;
import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import fr.enedis.chutney.campaign.infra.jpa.CampaignScenarioEntity;
import fr.enedis.chutney.dataset.infra.index.DatasetIndexRepository;
import fr.enedis.chutney.index.api.dto.Hit;
import fr.enedis.chutney.index.infra.CustemChutneyAnalyzer;
import fr.enedis.chutney.index.infra.LuceneIndexRepository;
import fr.enedis.chutney.index.infra.config.IndexConfig;
import fr.enedis.chutney.scenario.infra.index.ScenarioIndexRepository;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IndexRepositoryTest {

    private Directory directory;
    private IndexWriter indexWriter;

    private CampaignIndexRepository campaignRepository;
    private ScenarioIndexRepository scenarioRepository;
    private DatasetIndexRepository datasetRepository;

    @BeforeEach
    public void setUp() throws IOException {
        Analyzer analyzer = new CustemChutneyAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        directory = new ByteBuffersDirectory();
        indexWriter = new IndexWriter(directory, config);

        IndexConfig indexConfig = mock(IndexConfig.class);
        when(indexConfig.directory()).thenReturn(directory);
        when(indexConfig.indexWriter()).thenReturn(indexWriter);
        when(indexConfig.analyzer()).thenReturn(analyzer);

        LuceneIndexRepository luceneIndexRepository = new LuceneIndexRepository(indexConfig);

        campaignRepository = new CampaignIndexRepository(luceneIndexRepository);
        scenarioRepository = new ScenarioIndexRepository(luceneIndexRepository);
        datasetRepository = new DatasetIndexRepository(luceneIndexRepository);
    }

    @AfterEach
    public void tearDown() throws IOException {
        indexWriter.close();
        directory.close();
    }

    @Test
    public void testCampaignRepository() {
        CampaignEntity campaign = createCampaignEntity(2L, "Campaign Title", "", "", false, false, "", null, 1, null);
        campaignRepository.save(campaign);

        List<Hit> results = campaignRepository.search("Campaign");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().title()).isEqualTo("<mark>Campaign</mark> Title");
    }

    @Test
    public void testScenarioRepository() throws Exception {
        ScenarioEntity scenario = createScenarioEntity(1L, "Scenario Title", "Description of the Scenario", "Content with scenario described", "tag1,tag2");
        scenarioRepository.save(scenario);

        List<Hit> results = scenarioRepository.search("Scenario");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().title()).isEqualTo("<mark>Scenario</mark> Title");
        assertThat(results.getFirst().description()).isEqualTo("Description of the <mark>Scenario</mark>");
        assertThat(results.getFirst().content()).isEqualTo("Content with <mark>scenario</mark> described");
    }

    @Test
    public void testDatasetRepository() throws Exception {
        DataSet dataset = createDataSet("1", "Dataset Name", Map.of("A", "B"), List.of(Map.of("A", "A1", "B", "B1")));
        datasetRepository.save(dataset);

        List<Hit> results = datasetRepository.search("A");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().description()).isEqualTo("<mark>Dataset</mark> <mark>Name</mark>");
        assertThat(results.getFirst().content()).contains("<mark>");
    }

    @Test
    public void search_special_character() throws Exception {
        ScenarioEntity scenario = createScenarioEntity(1L, "~12345 OR #appel OR --copy", "Description of the Scenario", "Content with scenario described", "tag1,tag2");
        scenarioRepository.save(scenario);

        List<Hit> results = scenarioRepository.search("#");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().title()).isEqualTo("~12345 OR <mark>#appel</mark> OR --copy");
    }

    private CampaignEntity createCampaignEntity(Long id, String title, String description, String environment, boolean parallelRun, boolean retryAuto, String datasetId, List<String> tags, Integer version, List<CampaignScenarioEntity> campaignScenarios) {
        return new CampaignEntity(id, title, description, environment, parallelRun, retryAuto, datasetId, tags, version, campaignScenarios);
    }

    private ScenarioEntity createScenarioEntity(Long id, String title, String description, String content, String tags) {
        return new ScenarioEntity(id, title, description, content, tags, now(), true, null, now(), null, null);

    }

    private DataSet createDataSet(String name, String description, Map<String, String> constants, List<Map<String, String>> datatable) {
        return DataSet.builder()
            .withId(name)
            .withName(name)
            .withDescription(description)
            .withConstants(constants)
            .withDatatable(datatable)
            .build();
    }
}
