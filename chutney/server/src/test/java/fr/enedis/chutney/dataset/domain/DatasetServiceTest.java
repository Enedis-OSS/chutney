/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.domain;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.scenario.domain.gwt.GwtScenario;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.dataset.DataSetAlreadyExistException;
import fr.enedis.chutney.server.core.domain.scenario.AggregatedRepository;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DatasetServiceTest {

    private final DataSetRepository datasetRepository = mock(DataSetRepository.class);
    private final CampaignRepository campaignRepository = mock(CampaignRepository.class);
    private final AggregatedRepository<GwtTestCase> testCaseRepository = mock(AggregatedRepository.class);

    DatasetService sut = new DatasetService(datasetRepository, campaignRepository, testCaseRepository);

    @Test
    public void should_sort_dataset_by_name() {
        // Given
        DataSet firstDataset = DataSet.builder().withName("A").build();
        DataSet secondDataset = DataSet.builder().withName("B").build();
        DataSet thirdDataset = DataSet.builder().withName("C").build();

        when(datasetRepository.findAll())
            .thenReturn(List.of(thirdDataset, secondDataset, firstDataset));

        // When
        List<DataSet> actual = sut.findAll(false);

        // Then
        assertThat(actual).containsExactly(firstDataset, secondDataset, thirdDataset);
    }

    @Test
    public void should_get_all_dataset_with_scenario_usage() {
        // Given
        DataSet firstDataset = DataSet.builder().withName("A").withId("A").build();
        DataSet secondDataset = DataSet.builder().withName("B").withId("B").build();

        Campaign.CampaignScenario campaignScenario = new Campaign.CampaignScenario("Scenario1", "A");

        Campaign campaign1 = new Campaign(1L, "campaign1", "description", List.of(campaignScenario), "env", false, false, "A", List.of());
        Campaign campaign2 = new Campaign(1L, "campaign2", "description", List.of(), "env", false, false, "A", List.of());

        TestCaseMetadata testCaseMetadata = TestCaseMetadataImpl.builder().withTitle("Scenario1").withId("Scenario1").withDefaultDataset("A").build();

        GwtTestCase gwtTestCase = GwtTestCase.builder().withMetadata(TestCaseMetadataImpl.builder().withTitle("Scenario1").withId("testCaseId").build()).build();

        when(datasetRepository.findAll())
            .thenReturn(List.of(firstDataset, secondDataset));
        when(testCaseRepository.findAllByDatasetId("A"))
            .thenReturn(List.of(testCaseMetadata));
        when(campaignRepository.findAll())
            .thenReturn(List.of(campaign1, campaign2));
        when(testCaseRepository.findById("Scenario1"))
            .thenReturn(Optional.ofNullable(gwtTestCase));

        // When
        List<DataSet> actual = sut.findAll(true);

        // Then
        assertThat(actual).hasSize(2);
        assertThat(actual.getFirst().scenarioUsage).hasSize(1);
        assertThat(actual.getFirst().scenarioUsage).contains("Scenario1");
        assertThat(actual.getFirst().campaignUsage).hasSize(2);
        assertThat(actual.getFirst().campaignUsage).contains("campaign1");
        assertThat(actual.getFirst().campaignUsage).contains("campaign2");
        assertThat(actual.getFirst().scenarioInCampaignUsage).containsEntry("campaign1", Set.of("Scenario1"));
    }

    @Test
    void should_remove_deleted_dataset_from_campaigns_and_scenarios() {
        String datasetId = "dataset_id";

        TestCaseMetadataImpl metadata = TestCaseMetadataImpl.builder().withDefaultDataset(datasetId).build();
        when(testCaseRepository.findAll()).thenReturn(List.of(metadata));

        GwtTestCase testCase = GwtTestCase.builder().withMetadata(metadata).withScenario(mock(GwtScenario.class)).build();
        when(testCaseRepository.findById(any())).thenReturn(of(testCase));

        Campaign campaign = CampaignBuilder.builder()
            .setId(1L)
            .setTitle("Campaign")
            .setDescription("")
            .setEnvironment("Env")
            .setTags(List.of())
            .setDatasetId(datasetId)
            .build();
        when(campaignRepository.findAll()).thenReturn(List.of(campaign));

        GwtTestCase expectedScenario = GwtTestCase.builder().from(testCase).withMetadata(
            TestCaseMetadataImpl.TestCaseMetadataBuilder.from(metadata).withDefaultDataset(null).build()
        ).build();
        Campaign expectedCampaign = CampaignBuilder.builder().from(campaign).setDatasetId("").build();

        sut.remove(datasetId);

        verify(testCaseRepository).save(expectedScenario);
        verify(campaignRepository).createOrUpdate(expectedCampaign);
    }

    @Test
    public void should_create_dataset_when_not_existing() {
        // Given
        String name = "a dataset";
        DataSet dataset = DataSet.builder().withName(name).build();

        when(datasetRepository.save(any()))
            .thenReturn("a_dataset");
        when(datasetRepository.existByName(name))
            .thenReturn(false);

        // When
        DataSet persistedDataset = sut.create(dataset);

        // Then
        assertThat(persistedDataset.id).isEqualTo("a_dataset");
    }
    @Test
    void should_return_dataset_by_id() {
        DataSet expected = DataSet.builder().withId("123").withName("Test").build();
        when(datasetRepository.findById("123")).thenReturn(expected);

        DataSet actual = sut.findById("123");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_throw_exception_when_creating_existing_dataset() {
        DataSet existing = DataSet.builder().withId("existing").withName("existing").build();
        when(datasetRepository.existByName("existing")).thenReturn(true);

        DataSet newDataset = DataSet.builder().withName("existing").build();

        assertThatThrownBy(() -> sut.create(newDataset))
            .isInstanceOf(DataSetAlreadyExistException.class)
            .hasMessageContaining("already exists");
    }


    @Test
    void should_update_dataset_and_references_when_renaming() {
        String oldId = "old_dataset";
        String newId = "new_dataset";
        String newName = "new dataset";

        TestCaseMetadataImpl metadata = TestCaseMetadataImpl.builder()
            .withId("tc1")
            .withDefaultDataset(oldId)
            .build();

        GwtTestCase testCase = GwtTestCase.builder()
            .withMetadata(metadata)
            .withScenario(mock(GwtScenario.class))
            .build();

        Campaign campaign = CampaignBuilder.builder()
            .setId(1L)
            .setTitle("Campaign")
            .setDescription("")
            .setEnvironment("Env")
            .setTags(List.of())
            .setDatasetId(oldId)
            .build();

        DataSet updatedDataset = DataSet.builder().withId(oldId).withName(newName).build();

        when(testCaseRepository.findAll()).thenReturn(List.of(metadata));
        when(testCaseRepository.findById("tc1")).thenReturn(Optional.of(testCase));
        when(datasetRepository.save(updatedDataset)).thenReturn(newId);
        when(campaignRepository.findAll()).thenReturn(List.of(campaign));

        sut.update(oldId, updatedDataset);

        verify(datasetRepository).save(updatedDataset);
        verify(datasetRepository).removeById(oldId);

        ArgumentCaptor<GwtTestCase> captor = ArgumentCaptor.forClass(GwtTestCase.class);
        verify(testCaseRepository).save(captor.capture());
        GwtTestCase saved = captor.getValue();
        assertThat(saved.metadata.defaultDataset()).isEqualTo(newId);

        verify(campaignRepository).createOrUpdate(
            CampaignBuilder.builder().from(campaign).setDatasetId(newId).build()
        );
    }

    @Test
    void should_update_only_dataset_when_no_renaming() {
        String name = "old dataset";
        String id = "old_dataset";
        DataSet dataSet = DataSet.builder().withId(id).withName(name).build();

        when(datasetRepository.save(dataSet)).thenReturn(id);

        sut.update(id, dataSet);

        verify(datasetRepository).save(dataSet);
        verify(datasetRepository, never()).removeById(any());
        verify(testCaseRepository, never()).findAll();
        verify(campaignRepository, never()).findAll();
    }



}
