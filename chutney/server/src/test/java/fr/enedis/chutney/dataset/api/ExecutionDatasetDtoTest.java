/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.api;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionDatasetDtoTest {
    @Test
    void compute_dataset_emptiness() {
        var sut = new ExecutionDatasetDto();

        sut.setConstants(null);
        sut.setDatatable(null);
        assertThat(sut.isEmpty()).isTrue();

        sut.setConstants(emptyList());
        sut.setDatatable(emptyList());
        assertThat(sut.isEmpty()).isTrue();

        sut.setConstants(List.of(ImmutableKeyValue.builder().key("").build()));
        sut.setDatatable(List.of(List.of(ImmutableKeyValue.builder().key("").build())));
        assertThat(sut.isEmpty()).isFalse();
    }
}
