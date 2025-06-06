/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.scenario.campaign;

import java.util.ArrayList;
import java.util.List;

public class Campaign {

    public final Long id;
    public final String title;
    public final String description;
    public final List<CampaignScenario> scenarios;
    public final boolean parallelRun;
    public final boolean retryAuto;
    public final List<String> tags;

    private String environment;
    private String datasetId;

    public Campaign(Long id,
                    String title,
                    String description,
                    List<CampaignScenario> scenarios,
                    String environment,
                    boolean parallelRun,
                    boolean retryAuto,
                    String datasetId,
                    List<String> tags) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.scenarios = initListNullOrEmpty(scenarios);
        this.parallelRun = parallelRun;
        this.retryAuto = retryAuto;
        this.environment = environment;
        this.datasetId = datasetId;
        this.tags = tags;
    }

    public void executionEnvironment(String environment) {
        this.environment = environment;
    }

    public void executionDataset(String datasetId) {
        this.datasetId = datasetId;
    }

    public String executionEnvironment() {
        return this.environment;
    }

    public String executionDataset() {
        return this.datasetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Campaign campaign = (Campaign) o;
        return id.equals(campaign.id) &&
            title.equals(campaign.title) &&
            description.equals(campaign.description) &&
            scenarios.equals(campaign.scenarios) &&
            parallelRun == campaign.parallelRun &&
            retryAuto == campaign.retryAuto &&
            environment.equals(campaign.environment) &&
            datasetId.equals(campaign.datasetId) &&
            tags.equals(campaign.tags);
    }

    private <T> List<T> initListNullOrEmpty(List<T> list) {
        if (list != null && !list.isEmpty()) {
            return list;
        }
        return new ArrayList<>();
    }

    public record CampaignScenario(String scenarioId, String datasetId) {
        public CampaignScenario(String scenarioId) {
            this(scenarioId, null);
        }
    }
}
