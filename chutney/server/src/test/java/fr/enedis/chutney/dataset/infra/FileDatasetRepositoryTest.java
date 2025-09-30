/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import fr.enedis.chutney.dataset.domain.DataSetRepository;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.dataset.DataSetNotFoundException;
import fr.enedis.chutney.tools.file.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class FileDatasetRepositoryTest {

    private static final String TMP_PATH = org.assertj.core.util.Files.temporaryFolderPath();
    private static final String STORE_PATH = TMP_PATH + File.separator + FileDatasetRepository.ROOT_DIRECTORY_NAME;
    private final DataSetRepository sut = new FileDatasetRepository(TMP_PATH);

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.cleanFolder(Paths.get(STORE_PATH));
    }

    @Test
    void should_create_dataset() {
        // Create
        DataSet dataset = DataSet.builder().withName("name").build();
        String id = sut.save(dataset);
        assertThat(Files.exists(Paths.get(STORE_PATH + File.separator + id + ".json"))).isTrue();
    }

    @Test
    void should_update_dataset() {
        // Create
        String name = "name";
        DataSet dataset = DataSet.builder().withName(name).build();
        String id = sut.save(dataset);
        assertThat(Files.exists(Paths.get(STORE_PATH + File.separator + id + ".json"))).isTrue();

        // Update
        DataSet updatedDataset = DataSet.builder().withId(id).withDescription("new description").withName(name).build();
        sut.save(updatedDataset);

        DataSet inDb = sut.findById(id);
        assertThat(inDb.description).isEqualTo("new description");
    }

    @Test
    void should_throw_exception_when_dataset_not_found() {
      String datasetId = "UNKNOWED_DATASET";
      assertThatThrownBy(() -> sut.findById(datasetId))
          .isInstanceOf(DataSetNotFoundException.class);
    }


    @Test
    void should_check_if_dataset_exists_by_name() {
        DataSet dataset = DataSet.builder().withName("existing").build();
        sut.save(dataset);

        boolean exists = sut.existByName("existing");
        boolean nonExisting = sut.existByName("non_existing");

        assertThat(exists).isTrue();
        assertThat(nonExisting).isFalse();
    }

    @Test
    void should_remove_dataset_by_id() {
        DataSet dataset = DataSet.builder().withName("to_delete").build();
        String id = sut.save(dataset);

        sut.removeById(id);

        assertThat(Files.exists(Paths.get(STORE_PATH + File.separator + id + ".json"))).isFalse();
    }

    @Test
    void should_return_all_datasets() {
        sut.save(DataSet.builder().withName("one").build());
        sut.save(DataSet.builder().withName("two").build());

        List<DataSet> all = sut.findAll();

        assertThat(all).hasSize(2);
        assertThat(all.stream().map(ds -> ds.name)).containsExactlyInAnyOrder("one", "two");
    }
}
