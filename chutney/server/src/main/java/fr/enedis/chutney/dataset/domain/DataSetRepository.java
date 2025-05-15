/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.domain;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import java.util.List;

public interface DataSetRepository {

    String save(DataSet dataSet);

    DataSet findById(String dataSetId);

    void removeById(String dataSetId);

    List<DataSet> findAll();
}
