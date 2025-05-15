/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.infrastructure.delegation;

import fr.enedis.chutney.engine.api.execution.DatasetDto;
import fr.enedis.chutney.engine.domain.execution.engine.Dataset;

public class DatasetMapper {

    static DatasetDto toDto(Dataset dataset) {
        return new DatasetDto(dataset.constants, dataset.datatable);
    }

}
