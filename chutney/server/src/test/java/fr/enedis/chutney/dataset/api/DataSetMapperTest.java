/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.api;

import static fr.enedis.chutney.dataset.api.DataSetMapper.fromDto;
import static fr.enedis.chutney.dataset.api.DataSetMapper.toDto;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DataSetMapperTest {

    // TODO issue https://github.com/chutney-testing/chutney-legacy/issues/532 for more details
    @Test
    @Disabled
    void constants_order_must_be_kept() {
        List<KeyValue> constants = new ArrayList<>();
        IntStream.range(1, 10).mapToObj(i -> keyOf("key" + i, "v" + i)).forEachOrdered(constants::add);

        DataSetDto dataSetDto = ImmutableDataSetDto.builder()
            .id("id")
            .name("name")
            .addAllConstants(constants)
            .build();

        DataSetDto dataset = toDto(fromDto(dataSetDto));

        assertThat(dataset.constants()).containsExactlyElementsOf(constants);
    }

    @Test
        //@Disabled
    void datatable_line_order_must_be_kept() {
        List<List<KeyValue>> datatable = List.of(
            List.of(keyOf("col1", "v11"), keyOf("col2", "v12"), keyOf("col3", "v13"), keyOf("col4", "v14")),
            List.of(keyOf("col1", "v21"), keyOf("col2", "v22"), keyOf("col3", "v23"), keyOf("col4", "v24")),
            List.of(keyOf("col1", "v31"), keyOf("col2", "v32"), keyOf("col3", "v33"), keyOf("col4", "v34")),
            List.of(keyOf("col1", "v41"), keyOf("col2", "v42"), keyOf("col3", "v43"), keyOf("col4", "v44"))
        );
        DataSetDto dataSetDto = ImmutableDataSetDto.builder()
            .id("id")
            .name("name")
            .addAllDatatable(datatable)
            .build();

        DataSetDto dataset = toDto(fromDto(dataSetDto));

        assertThat(dataset.datatable()).hasSize(4).satisfies(dt -> {
            for (int i = 0; i < 4; i++) {
                assertThat(dt.get(i)).containsExactlyInAnyOrderElementsOf(datatable.get(i));
            }
        });
    }

    @Test
    @Disabled
    void datatable_column_order_must_be_kept() {
        List<KeyValue> datatableLine = List.of(keyOf("col1", "v11"), keyOf("col2", "v12"), keyOf("col3", "v13"), keyOf("col4", "v14"));
        DataSetDto dataSetDto = ImmutableDataSetDto.builder()
            .id("id")
            .name("name")
            .addDatatable(datatableLine)
            .build();

        DataSetDto dataset = toDto(fromDto(dataSetDto));

        assertThat(dataset.datatable().getFirst()).containsExactlyElementsOf(datatableLine);
    }

    @Test
    public void build_dataset_with_every_field() {
        DataSet dataSet = DataSet.builder()
            .withName("dataset")
            .withId("id")
            .withDatatable(List.of(Map.of("TOTO", "TUTU")))
            .withConstants(Map.of("TOTO", "TUTU"))
            .withDescription("description")
            .withCreationDate(Instant.now())
            .withTags(List.of("TAG"))
            .withCampaignUsage(Set.of("TOTO"))
            .withScenarioInCampaign(Map.of("TUTU", Set.of("BIBI")))
            .withScenarioUsage(Set.of("JEJE"))
            .build();

        DataSetDto datasetDto = DataSetMapper.toDto(dataSet);

        assertThat(datasetDto).isNotNull();
        assertThat(datasetDto.constants().getFirst().key()).isEqualTo("TOTO");
        assertThat(datasetDto.constants().getFirst().value()).isEqualTo("TUTU");
        assertThat(datasetDto.datatable().getFirst().getFirst().key()).isEqualTo("TOTO");
        assertThat(datasetDto.datatable().getFirst().getFirst().value()).isEqualTo("TUTU");
        assertThat(datasetDto.tags().getFirst()).isEqualTo("TAG");
        assertThat(datasetDto.description()).isEqualTo("description");
        assertThat(datasetDto.lastUpdated()).isAfter(Instant.now().minus(1, ChronoUnit.HOURS));
        assertThat(datasetDto.id().get()).isEqualTo("id");
        assertThat(datasetDto.name()).isEqualTo("dataset");
        assertThat(datasetDto.campaignUsage()).containsExactly("TOTO");
        assertThat(datasetDto.scenarioUsage()).containsExactly("JEJE");
        assertThat(datasetDto.scenarioInCampaignUsage()).containsEntry("TUTU", Set.of("BIBI"));
    }

    @Test
    public void build_dataset_from_NO_DATASET() {
        DataSet dataSet = DataSet.NO_DATASET;

        DataSetDto datasetDto = DataSetMapper.toDto(dataSet);

        assertThat(datasetDto).isNotNull();
    }

    @Test
    public void build_dataset_from_execution_dataset() {
        ExecutionDatasetDto executionDatasetDto = new ExecutionDatasetDto()
            .setConstants(List.of(ImmutableKeyValue.builder().key("TOTO").value("TUTU").build()))
            .setDatatable(List.of(List.of(ImmutableKeyValue.builder().key("TOTO").value("TUTU").build())));

        DataSet dataset = DataSetMapper.fromExecutionDatasetDto(executionDatasetDto, id -> DataSet.NO_DATASET);

        assertThat(dataset).isNotNull();
        assertThat(dataset.constants).containsKey("TOTO");
        assertThat(dataset.constants).containsValue("TUTU");
        assertThat(dataset.datatable.getFirst()).containsKey("TOTO");
        assertThat(dataset.datatable.getFirst()).containsValue("TUTU");
        assertThat(dataset.id).isNull();
        assertThat(dataset.name).isEqualTo("");
    }

    @Test
    public void get_dataset_from_execution_dataset_id() {
        ExecutionDatasetDto executionDatasetDto = new ExecutionDatasetDto()
            .setConstants(List.of(ImmutableKeyValue.builder().key("TOTO").value("TUTU").build()))
            .setDatatable(List.of(List.of(ImmutableKeyValue.builder().key("TOTO").value("TUTU").build())))
            .setId("dataset");

        DataSet getDS = DataSet.builder()
            .withConstants(Map.of("TATA", "TITI"))
            .withDatatable(List.of(Map.of("TATA", "TITI")))
            .withId("dataset")
            .withName("dataset")
            .build();

        DataSet dataset = DataSetMapper.fromExecutionDatasetDto(executionDatasetDto, id -> getDS);

        assertThat(dataset).isNotNull();
        assertThat(dataset.constants).containsKey("TATA");
        assertThat(dataset.constants).containsValue("TITI");
        assertThat(dataset.datatable.getFirst()).containsKey("TATA");
        assertThat(dataset.datatable.getFirst()).containsValue("TITI");
        assertThat(dataset.id).isEqualTo("dataset");
        assertThat(dataset.name).isEqualTo("dataset");
    }

    @Test
    public void build_dataset_from_execution_dataset_when_custom_id() {
        ExecutionDatasetDto executionDatasetDto = new ExecutionDatasetDto()
            .setConstants(List.of(ImmutableKeyValue.builder().key("TOTO").value("TUTU").build()))
            .setDatatable(List.of(List.of(ImmutableKeyValue.builder().key("TOTO").value("TUTU").build())))
            .setId(DataSet.CUSTOM_ID);

        DataSet getDS = DataSet.builder()
            .withConstants(Map.of("TATA", "TITI"))
            .withDatatable(List.of(Map.of("TATA", "TITI")))
            .withId("dataset")
            .withName("dataset")
            .build();

        DataSet dataset = DataSetMapper.fromExecutionDatasetDto(executionDatasetDto, id -> getDS);

        assertThat(dataset).isNotNull();
        assertThat(dataset.constants).containsKey("TOTO");
        assertThat(dataset.constants).containsValue("TUTU");
        assertThat(dataset.datatable.getFirst()).containsKey("TOTO");
        assertThat(dataset.datatable.getFirst()).containsValue("TUTU");
        assertThat(dataset.id).isEqualTo(DataSet.CUSTOM_ID);
        assertThat(dataset.name).isEqualTo("");
    }

    private KeyValue keyOf(String key, String value) {
        return ImmutableKeyValue.builder().key(key).value(value).build();
    }
}
