/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package util.infra;

import static util.infra.AbstractLocalDatabaseTest.DB_CHANGELOG_DB_CHANGELOG_MASTER_XML;

import fr.enedis.chutney.ServerConfiguration;
import fr.enedis.chutney.execution.infra.aop.ExecutionReportIndexingAspect;
import fr.enedis.chutney.execution.infra.storage.DatabaseExecutionJpaRepository;
import fr.enedis.chutney.execution.infra.storage.index.ExecutionReportIndexRepository;
import fr.enedis.chutney.index.infra.LuceneIndexRepository;
import fr.enedis.chutney.index.infra.config.IndexConfig;
import fr.enedis.chutney.index.infra.config.OnDiskIndexConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.UpdateSummaryEnum;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.logging.core.AbstractLogService;
import liquibase.logging.core.AbstractLogger;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.sqlite.SQLiteConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import util.SocketUtil;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@EnableJpa
@EnableAspectJAutoProxy
@Profile("test-infra")
class TestInfraConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestInfraConfiguration.class);

    @Configuration
    @Profile("test-infra-h2")
    static class H2Configuration {
        private static final Logger LOGGER = LoggerFactory.getLogger(H2Configuration.class);

        @Bean
        public DataSourceProperties inMemoryDataSourceProperties() {
            DataSourceProperties dataSourceProperties = new DataSourceProperties();
            dataSourceProperties.setUrl("jdbc:h2:mem:test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
            return dataSourceProperties;
        }

        @Bean
        @Profile("test-infra-h2-file")
        @Primary
        public DataSourceProperties fileDataSourceProperties(Server h2Server) {
            DataSourceProperties dataSourceProperties = new DataSourceProperties();
            dataSourceProperties.setUrl("jdbc:h2:tcp://localhost:" + h2Server.getPort() + "/./h2-chutney;SCHEMA=PUBLIC");
            return dataSourceProperties;
        }

        @Bean
        public Properties jpaProperties() {
            Properties jpaProperties = new Properties();
            jpaProperties.putAll(Map.of(
                "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
                "hibernate.show_sql", "false",
                "hibernate.use-new-id-generator-mappings", "false",
                "hibernate.cache.use_second_level_cache", "true",
                "hibernate.cache.use_query_cache", "true",
                "hibernate.region.factory_class", "org.hibernate.cache.jcache.internal.JCacheRegionFactory",
                "hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider",
                "hibernate.javax.cache.uri", "ehcache.xml"
            ));
            return jpaProperties;
        }

        @Bean(value = "dbServer", destroyMethod = "stop")
        @Profile("test-infra-h2-file")
        Server dbServer() throws SQLException, IOException {
            int availablePort = SocketUtil.freePort();
            Path tempDirectory = Files.createTempDirectory("test-infra-h2-");
            Server h2Server = Server.createTcpServer("-tcp", "-tcpPort", String.valueOf(availablePort), "-tcpAllowOthers", "-baseDir", tempDirectory.toString(), "-ifNotExists").start();
            LOGGER.debug("Started H2 server " + h2Server.getURL());
            return h2Server;
        }
    }

    @Configuration
    @Profile("test-infra-sqlite")
    static class SQLiteConfiguration {
        @Bean
        public DataSourceProperties dataSourceProperties() throws IOException {
            DataSourceProperties dataSourceProperties = new DataSourceProperties();
            Path tmpDir = Files.createTempDirectory("test-infra-sqlite-");
            dataSourceProperties.setUrl("jdbc:sqlite:" + tmpDir.toAbsolutePath() + "/sqlitesample.db");
            return dataSourceProperties;
        }

        @Bean
        public Properties jpaProperties() {
            Properties jpaProperties = new Properties();
            jpaProperties.putAll(Map.of(
                "hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect",
                "hibernate.show_sql", "false",
                "hibernate.use-new-id-generator-mappings", "false",
                "hibernate.cache.use_second_level_cache", "true",
                "hibernate.cache.use_query_cache", "true",
                "hibernate.region.factory_class", "org.hibernate.cache.jcache.internal.JCacheRegionFactory",
                "hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider",
                "hibernate.javax.cache.uri", "ehcache.xml"
            ));
            return jpaProperties;
        }

        @Bean
        public DataSource dataSource(
            DataSourceProperties dataSourceProperties,
            @Value("${sqlite.config.lockingMode:NORMAL}") SQLiteConfig.LockingMode lockingMode,
            @Value("${sqlite.config.transactionMode:IMMEDIATE}") SQLiteConfig.TransactionMode transactionMode,
            @Value("${sqlite.config.journalMode:WAL}") SQLiteConfig.JournalMode journalMode,
            @Value("${sqlite.config.synchronousMode:NORMAL}") SQLiteConfig.SynchronousMode synchronousMode,
            @Value("${sqlite.config.journalSizeLimit:10485760}") int journalSizeLimit
        ) {
            LOGGER.info("test configuration datasource : {}", dataSourceProperties.getUrl());
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setJdbcUrl(dataSourceProperties.getUrl());

            SQLiteConfig sqLiteConfig = new SQLiteConfig();
            sqLiteConfig.setLockingMode(lockingMode);
            sqLiteConfig.setTransactionMode(transactionMode);
            sqLiteConfig.setJournalMode(journalMode);
            sqLiteConfig.setSynchronous(synchronousMode);
            sqLiteConfig.setJournalSizeLimit(journalSizeLimit);

            hikariConfig.setDataSourceProperties(sqLiteConfig.toProperties());
            return new HikariDataSource(hikariConfig);
        }
    }

    @Configuration
    @Profile("test-infra-pgsql")
    static class PostgresConfiguration {

        @Bean(initMethod = "start", destroyMethod = "stop")
        public PostgreSQLContainer postgresDB() {
            return new PostgreSQLContainer(DockerImageName.parse("postgres:10.23-bullseye"));
        }

        @Bean
        public DataSourceProperties dataSourceProperties(PostgreSQLContainer postgresDB) {
            DataSourceProperties dataSourceProperties = new DataSourceProperties();
            dataSourceProperties.setUrl(postgresDB.getJdbcUrl());
            dataSourceProperties.setUsername(postgresDB.getUsername());
            dataSourceProperties.setPassword(postgresDB.getPassword());
            return dataSourceProperties;
        }

        @Bean
        public Properties jpaProperties() {
            Properties jpaProperties = new Properties();
            jpaProperties.putAll(Map.of(
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.show_sql", "false",
                "hibernate.cache.use_second_level_cache", "true",
                "hibernate.cache.use_query_cache", "true",
                "hibernate.region.factory_class", "org.hibernate.cache.jcache.internal.JCacheRegionFactory",
                "hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider",
                "hibernate.javax.cache.uri", "ehcache.xml"
            ));
            return jpaProperties;
        }
    }

    @Bean
    public ObjectMapper reportObjectMapper() {
        return new ServerConfiguration().reportObjectMapper();
    }

    @Bean
    public LuceneIndexRepository reportLuceneIndexRepository(IndexConfig reportIndexConfig) {
        return new LuceneIndexRepository(reportIndexConfig);
    }

    @Bean
    public LuceneIndexRepository scenarioLuceneIndexRepository(IndexConfig scenarioIndexConfig) {
        return new LuceneIndexRepository(scenarioIndexConfig);
    }

    @Bean
    public LuceneIndexRepository datasetLuceneIndexRepository(IndexConfig datasetIndexConfig) {
        return new LuceneIndexRepository(datasetIndexConfig);
    }

    @Bean
    public LuceneIndexRepository campaignLuceneIndexRepository(IndexConfig campaignIndexConfig) {
        return new LuceneIndexRepository(campaignIndexConfig);
    }

    @Bean
    public IndexConfig reportIndexConfig() throws IOException {
        Path tempDirectory = Files.createTempDirectory("test-report-index");
        return new OnDiskIndexConfig(tempDirectory.toString(), "report");
    }

    @Bean
    public IndexConfig scenarioIndexConfig() throws IOException{
        Path tempDirectory = Files.createTempDirectory("test-scenario-index");
        return new OnDiskIndexConfig(tempDirectory.toString(), "scenario");
    }

    @Bean
    public IndexConfig datasetIndexConfig() throws IOException{
        Path tempDirectory = Files.createTempDirectory("test-dataset-index");
        return new OnDiskIndexConfig(tempDirectory.toString(), "dataset");
    }

    @Bean
    public IndexConfig campaignIndexConfig() throws IOException{
        Path tempDirectory = Files.createTempDirectory("test-campaign-index");
        return new OnDiskIndexConfig(tempDirectory.toString(), "campaign");
    }

    @Bean
    public ExecutionReportIndexingAspect indexingAspect(ExecutionReportIndexRepository indexRepository, DatabaseExecutionJpaRepository scenarioExecutionsJpaRepository) {
        return new ExecutionReportIndexingAspect(indexRepository, scenarioExecutionsJpaRepository);
    }

    @Primary
    @Bean
    @Profile("!test-infra-sqlite")
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        LOGGER.info("test configuration datasource : {}", dataSourceProperties.getUrl());
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setJdbcUrl(dataSourceProperties.getUrl());
        hikariConfig.setUsername(dataSourceProperties.getUsername());
        hikariConfig.setPassword(dataSourceProperties.getPassword());
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, Properties jpaProperties) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("fr.enedis.chutney");
        factory.setDataSource(dataSource);
        factory.setJpaProperties(jpaProperties);
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        return txManager;
    }

    @Bean
    public Liquibase liquibase(
        DataSource ds,
        @Value("${chutney.test-infra.liquibase.run:true}") boolean liquibaseInit,
        @Value("${chutney.test-infra.liquibase.context:!test}") String initContext,
        @Value("${chutney.test-infra.liquibase.log.service:false}") boolean logService,
        @Value("${chutney.test-infra.liquibase.log.ui:false}") boolean logUi,
        @Value("${chutney.test-infra.liquibase.log.summary:false}") boolean logSummary
    ) throws Exception {
        try (Connection conn = ds.getConnection()) {
            Database liquibaseDB = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
            Liquibase liquibase = new Liquibase(DB_CHANGELOG_DB_CHANGELOG_MASTER_XML, new ClassLoaderResourceAccessor(), liquibaseDB);
            if (!logService) {
                Scope.enter(Map.of(Scope.Attr.logService.name(), new NoLiquibaseLogService()));
            }
            if (!logUi) {
                Scope.enter(Map.of(Scope.Attr.ui.name(), new LoggerUIService()));
            }
            if (!logSummary) {
                liquibase.setShowSummary(UpdateSummaryEnum.OFF);
            }
            if (liquibaseInit) {
                liquibase.update(initContext);
            }
            return liquibase;
        }
    }

    private static class NoLiquibaseLogService extends AbstractLogService {

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public liquibase.logging.Logger getLog(Class clazz) {
            return new AbstractLogger() {
                @Override
                public void log(Level level, String message, Throwable e) {

                }
            };
        }
    }
}
