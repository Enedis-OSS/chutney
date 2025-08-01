/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.util.Lists.newArrayList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.enedis.chutney.campaign.domain.CampaignExecutionRepository;
import fr.enedis.chutney.campaign.domain.CampaignNotFoundException;
import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.assertj.core.util.Lists;

public class FakeCampaignRepository implements CampaignRepository, CampaignExecutionRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, Campaign> campaignsById = new HashMap<>();
    private final Map<Long, List<CampaignExecution>> campaignsExecutionById = new HashMap<>();
    private final Multimap<String, Campaign> campaignsByName = ArrayListMultimap.create();

    @Override
    public Campaign createOrUpdate(Campaign campaign) {
        final Campaign saved;
        if (campaign.id != null && campaignsById.containsKey(campaign.id)) {
            saved = campaign;
        } else {
            saved = new Campaign(sequence.incrementAndGet(), campaign.title, campaign.description, campaign.scenarios, "env", false, false, null, campaign.tags);
        }
        campaignsById.put(saved.id, saved);
        campaignsByName.put(saved.title, saved);

        return saved;
    }

    @Override
    public void saveCampaignExecution(Long campaignId, CampaignExecution execution) {
        ofNullable(campaignsById.get(campaignId)).ifPresent(campaign -> {
            Campaign c = new Campaign(campaign.id, campaign.title, campaign.title, campaign.scenarios, campaign.executionEnvironment(), false, false, null, null);
            createOrUpdate(c);
        });

        List<CampaignExecution> foundReport = campaignsExecutionById.get(campaignId);
        if (foundReport == null) {
            foundReport = Lists.newArrayList();
        }
        foundReport.add(execution);
        campaignsExecutionById.put(campaignId, foundReport);
    }

    @Override
    public boolean removeById(Long id) {
        return campaignsById.remove(id) != null;
    }

    @Override
    public Campaign findById(Long campaignId) throws CampaignNotFoundException {
        if (!campaignsById.containsKey(campaignId)) {
            throw new CampaignNotFoundException(campaignId);
        }
        return campaignsById.get(campaignId);
    }

    @Override
    public List<Campaign> findAll() {
        return newArrayList(campaignsById.values());
    }

    @Override
    public List<Campaign> findByName(String campaignName) {
        return newArrayList(campaignsByName.get(campaignName));
    }

    @Override
    public List<CampaignExecution> getExecutionHistory(Long campaignId) {
        return ofNullable(campaignsExecutionById.get(campaignId)).orElse(newArrayList());
    }

    @Override
    public List<CampaignExecution> getLastExecutions(Long numberOfExecution) {
        List<CampaignExecution> allExecutions = campaignsExecutionById.entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .sorted(executionComparatorReportByExecutionId())
            .collect(Collectors.toList());

        if (numberOfExecution < allExecutions.size()) {
            return allExecutions.subList(0, numberOfExecution.intValue());
        } else {
            return allExecutions;
        }
    }

    @Override
    public List<String> findScenariosIds(Long campaignId) {
        return campaignsById.get(campaignId).scenarios.stream().map(Campaign.CampaignScenario::scenarioId).toList();
    }

    @Override
    public Long generateCampaignExecutionId(Long campaignId, String environment, DataSet dataset) {
        return new Random(100).nextLong();
    }

    @Override
    public CampaignExecution getCampaignExecutionById(Long campaignExecutionId) {
        throw new NotImplementedException();
    }

    @Override
    public List<Campaign> findCampaignsByScenarioId(String scenarioId) {
        return campaignsById.values().stream()
            .filter(campaign -> campaign.scenarios.stream().anyMatch(cs -> scenarioId.equals(cs.scenarioId())))
            .collect(Collectors.toList());
    }

    @Override
    public List<Campaign> findCampaignsByEnvironment(String environment) {
        // not needed in tests
        return emptyList();
    }

    @Override
    public List<Campaign> findCampaignsByDatasetId(String datasetId) {
        // not needed in tests
        return emptyList();
    }

    @Override
    public List<CampaignExecution> currentExecutions(Long campaignId) {
        // not needed in tests
        return emptyList();
    }

    @Override
    public void startExecution(Long campaignId, CampaignExecution campaignExecution) {
        // not needed in tests
    }

    @Override
    public void stopExecution(Long campaignId, String environment) {
        // not needed in tests
    }

    @Override
    public CampaignExecution getLastExecution(Long campaignId) {
        // not needed in tests
        return null;
    }

    @Override
    public void deleteExecutions(Set<Long> executionsIds) {
        // not needed in tests
    }

    @Override
    public void clearAllExecutionHistory(Long id) {
        // not needed in tests
    }

    // Duplicate of fr.enedis.chutney.design.api.campaign.CampaignController#executionComparatorReportByExecutionId
    private static Comparator<CampaignExecution> executionComparatorReportByExecutionId() {
        return Comparator.<CampaignExecution>comparingLong(value -> value.executionId).reversed();
    }
}
