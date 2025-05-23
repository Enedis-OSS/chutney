/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.domain;

import static org.apache.commons.lang3.StringUtils.isBlank;

import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.dataset.DataSetNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.AggregatedRepository;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class DatasetService {

    private final DataSetRepository datasetRepository;
    private final CampaignRepository campaignRepository;
    private final AggregatedRepository<GwtTestCase> testCaseRepository;

    public DatasetService(DataSetRepository dataSetRepository, CampaignRepository campaignRepository, AggregatedRepository<GwtTestCase> testCaseRepository) {
        this.datasetRepository = dataSetRepository;
        this.campaignRepository = campaignRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public DataSet findById(String id) {
        return datasetRepository.findById(id);
    }

    public List<DataSet> findAll(Boolean usage) {
        Stream<DataSet> datasets = datasetRepository.findAll()
            .stream()
            .sorted(DataSet.datasetComparator);
        if (!usage) {
            return datasets.toList();
        }
        return datasets
            .map(dataset -> {
                List<Campaign> campaigns = campaignRepository.findAll();
                Set<String> campaignsUsingDataset = campaigns
                    .stream()
                    .filter(campaign -> dataset.id.equals(campaign.executionDataset()))
                    .map(campaign -> campaign.title)
                    .collect(Collectors.toSet());

                Map<String, Set<String>> scenarioInCampaignUsingDataset = campaigns.stream()
                    .filter(campaign -> campaign.scenarios.stream()
                        .anyMatch(scenario -> dataset.id.equals(scenario.datasetId())))
                    .collect(Collectors.groupingBy(
                        campaign -> campaign.title,
                        Collectors.flatMapping(
                            campaign -> campaign.scenarios.stream()
                                .filter(campaignScenario -> dataset.id.equals(campaignScenario.datasetId()))
                                .map(campaignScenario -> testCaseRepository.findById(campaignScenario.scenarioId()))
                                .map(scenario -> scenario.map(s -> s.metadata.title).orElseThrow()),
                            Collectors.toSet()
                        )
                    ));
                Set<String> scenariosUsingDataset = testCaseRepository.findAllByDatasetId(dataset.id)
                    .stream()
                    .map(TestCaseMetadata::title)
                    .collect(Collectors.toSet());
                return DataSet.builder()
                    .fromDataSet(dataset)
                    .withCampaignUsage(campaignsUsingDataset)
                    .withScenarioUsage(scenariosUsingDataset)
                    .withScenarioInCampaign(scenarioInCampaignUsingDataset)
                    .build();
            })
            .toList();
    }

    public DataSet save(DataSet dataset) {
        String id = datasetRepository.save(dataset);
        return DataSet.builder().fromDataSet(dataset).withId(id).build();
    }

    public DataSet updateWithRename(String oldId, DataSet dataset) {
        if (isBlank(oldId)) {
            throw new DataSetNotFoundException("");
        }

        DataSet newDataset = save(dataset);
        String newId = newDataset.id;
        if (!oldId.equals(newId)) {
            updateScenarios(oldId, newId);
            updateCampaigns(oldId, newId);
            datasetRepository.removeById(oldId);
        }
        return newDataset;
    }

    private void updateScenarios(String oldId, String newId) {
        testCaseRepository.findAll().stream()
            .filter(m -> oldId.equals(m.defaultDataset()))
            .map(m -> testCaseRepository.findById(m.id()))
            .forEach(o -> o.ifPresent(
                tc -> testCaseRepository.save(
                    GwtTestCase.builder()
                        .from(tc)
                        .withMetadata(TestCaseMetadataImpl.TestCaseMetadataBuilder.from(tc.metadata).withDefaultDataset(newId).build())
                        .build()
                ))
            );
    }

    private void updateCampaigns(String oldId, String newId) {
        campaignRepository.findAll().stream()
            .filter(c -> oldId.equals(c.executionDataset()))
            .forEach(c -> campaignRepository.createOrUpdate(
                CampaignBuilder.builder()
                    .from(c)
                    .setDatasetId(newId)
                    .build())
            );
    }


    public void remove(String datasetName) {
        datasetRepository.removeById(datasetName);
        updateScenarios(datasetName, "");
        updateCampaigns(datasetName, "");
    }

}
