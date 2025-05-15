/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.execution;

import static java.util.Optional.ofNullable;

import fr.enedis.chutney.engine.api.execution.DatasetDto;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;

public class DatasetMapper {

    static DatasetDto toDto(DataSet dataset) {
        return ofNullable(dataset)
            .map(d -> new DatasetDto(d.constants, d.datatable))
            .orElseGet(DatasetDto::new);
    }
}
