/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.scenario.campaign;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.tools.DatasetUtils;
import java.util.Objects;

public record TestCaseDataset(TestCase testcase, DataSet dataset) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCaseDataset that = (TestCaseDataset) o;
        return Objects.equals(testcase.id(), that.testcase.id()) && DatasetUtils.compareDataset(dataset, that.dataset());
    }

    @Override
    public int hashCode() {
        return Objects.hash(testcase.id(), dataset());
    }
}
