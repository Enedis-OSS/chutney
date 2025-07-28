/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra.jpa;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecutionReportBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CampaignExecutionEntityTest {

    @ParameterizedTest
    @ValueSource(strings = "__CUSTOM__") // DataSet.CUSTOM_ID
    @NullSource
    void set_custom_id_from_domain_when_build(String datasetId) {
        DataSet dataset = DataSet.builder().withId(datasetId).withName("").build();
        var sut = new CampaignExecutionEntity(1L, "env", dataset);
        CampaignExecution domainExecution = sut.toDomain("");
        assertThat(domainExecution.dataset.id).isEqualTo(DataSet.CUSTOM_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = "__CUSTOM__") // DataSet.CUSTOM_ID
    @NullSource
    void set_custom_id_from_domain_when_update(String datasetId) {
        DataSet dataset = DataSet.builder().withId(datasetId).withName("").build();
        var sut = new CampaignExecutionEntity();
        CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder().dataset(dataset).build();
        sut.updateFromDomain(campaignExecution, emptyList());
        CampaignExecution domainExecution = sut.toDomain("");
        assertThat(domainExecution.dataset.id).isEqualTo(DataSet.CUSTOM_ID);
    }
}
