/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package util.infra;

import static java.time.Instant.now;

import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import fr.enedis.chutney.campaign.infra.jpa.CampaignScenarioEntity;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.index.infra.LuceneIndexRepository;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.scenario.infra.raw.TagListMapper;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig
@ActiveProfiles("test-infra")
@ContextConfiguration(classes = TestInfraConfiguration.class)
public abstract class AbstractLocalDatabaseTest {

    private final Random rand = new Random();
    protected static final String DB_CHANGELOG_DB_CHANGELOG_MASTER_XML = "changelog/db.changelog-master.xml";
    @Autowired
    protected DataSource localDataSource;
    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    protected EntityManager entityManager;
    protected TransactionTemplate transactionTemplate = new TransactionTemplate();
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private Liquibase liquibase;

    @Autowired
    private List<LuceneIndexRepository> luceneIndexRepositories;

    @BeforeEach
    void setTransactionTemplate() {
        transactionTemplate.setTransactionManager(transactionManager);
    }

    protected void clearTables() {
        JdbcTemplate jdbcTemplate = namedParameterJdbcTemplate.getJdbcTemplate();
        jdbcTemplate.execute("DELETE FROM CAMPAIGN_EXECUTIONS");
        jdbcTemplate.execute("DELETE FROM SCENARIO_EXECUTIONS_REPORTS");
        jdbcTemplate.execute("DELETE FROM SCENARIO_EXECUTIONS");
        jdbcTemplate.execute("DELETE FROM CAMPAIGN_SCENARIOS");
        jdbcTemplate.execute("DELETE FROM CAMPAIGN");
        jdbcTemplate.execute("DELETE FROM SCENARIO");
        clearIndexes();

        // Clean caches
        entityManager.clear();
        entityManager.getEntityManagerFactory().getCache().evictAll();
    }

    private void clearIndexes() {
        luceneIndexRepositories.forEach(LuceneIndexRepository::deleteAll);
    }

    protected void liquibaseUpdate() throws LiquibaseException, SQLException {
        try (Connection conn = localDataSource.getConnection()) {
            liquibase.getDatabase().setConnection(new JdbcConnection(conn));
            liquibase.update("!test");
        }
    }

    protected ScenarioEntity givenScenario() {
        ScenarioEntity scenarioEntity = new ScenarioEntity(null, "", null, "{\"when\":{}}", TagListMapper.tagsToString(defaultScenarioTags()), now(), true, null, now(), null, null);
        return transactionTemplate.execute(ts -> {
            entityManager.persist(scenarioEntity);
            return scenarioEntity;
        });
    }

    protected String givenScenarioId() {
        int objectId = rand.nextInt(500);
        return String.valueOf(objectId);
    }

    protected CampaignEntity givenCampaign(ScenarioEntity... scenarioEntities) {
        ArrayList<CampaignScenarioEntity> campaignScenarioEntities = new ArrayList<>();
        CampaignEntity campaign = new CampaignEntity("", campaignScenarioEntities);
        return transactionTemplate.execute(ts -> {
            for (int i = 0; i < scenarioEntities.length; i++) {
                ScenarioEntity scenarioEntity = scenarioEntities[i];
                campaignScenarioEntities.add(new CampaignScenarioEntity(campaign, scenarioEntity.getId().toString(), null, i));
            }
            campaign.campaignScenarios().addAll(campaignScenarioEntities);
            entityManager.persist(campaign);
            return campaign;
        });
    }

    protected ScenarioExecutionEntity givenScenarioExecution(Long scenarioId, ServerReportStatus status) {
        ScenarioExecutionEntity execution = new ScenarioExecutionEntity(null, scenarioId.toString(), null, now().toEpochMilli(), 0L, status, null, null, "", "", "", null, TagListMapper.tagsToString(defaultScenarioTags()), null);
        return transactionTemplate.execute(ts -> {
            entityManager.persist(execution);
            return execution;
        });
    }

    protected List<Campaign.CampaignScenario> scenariosIds(ScenarioEntity... scenarioEntities) {
        return Arrays.stream(scenarioEntities).map(ScenarioEntity::getId).map(id -> new Campaign.CampaignScenario(id.toString(), null)).toList();
    }

    protected List<Campaign.CampaignScenario> scenariosIds(List<ScenarioEntity> scenarioEntities, List<String> datasetIds) {
        return IntStream.range(0, scenarioEntities.size())
            .mapToObj(idx -> new Campaign.CampaignScenario(scenarioEntities.get(idx).getId().toString(), datasetIds.get(idx)))
            .toList();
    }

    protected Set<String> defaultScenarioTags() {
        return Set.of("SIMPLE", "COM_PLEX");
    }
}
