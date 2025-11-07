/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.domain;

import static fr.enedis.chutney.index.domain.IndexObject.DATASET;
import static fr.enedis.chutney.index.domain.IndexObject.SCENARIO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class IndexServiceTest {

    @ParameterizedTest
    @EnumSource(IndexObject.class)
    void search_only_on_requested_objects_indexed(IndexObject requestedIndexedObject) {
        var scenarioIndexRepository = mock(IndexRepository.class);
        when(scenarioIndexRepository.indexObject()).thenReturn(SCENARIO);
        var datasetIndexRepository = mock(IndexRepository.class);
        when(datasetIndexRepository.indexObject()).thenReturn(DATASET);
        List<IndexRepository<?>> indexedRepositories = List.of(scenarioIndexRepository, datasetIndexRepository);
        IndexService sut = new IndexService(indexedRepositories);

        sut.search("query", Set.of(requestedIndexedObject));

        if (SCENARIO.equals(requestedIndexedObject)) {
            verify(scenarioIndexRepository).search(any());
            verify(datasetIndexRepository, times(0)).search(any());
        } else if (DATASET.equals(requestedIndexedObject)) {
            verify(scenarioIndexRepository, times(0)).search(any());
            verify(datasetIndexRepository).search(any());
            ;
        } else {
            verify(scenarioIndexRepository, times(0)).search(any());
            verify(datasetIndexRepository, times(0)).search(any());
        }
    }
}
