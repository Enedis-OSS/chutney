/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package changelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import util.infra.AbstractLocalDatabaseTest;
import util.infra.EnableH2MemTestInfra;
import util.infra.EnablePostgreSQLTestInfra;
import util.infra.EnableSQLiteTestInfra;

@DisplayName("Liquibase changelog")
class LiquibaseChangelogTest {

    @Nested
    @DisplayName("On a fresh new database")
    @TestPropertySource(properties = {"chutney.test-infra.liquibase.run=false"})
    class FreshDB {
        @Nested
        @EnableH2MemTestInfra
        class H2 extends AbstractLocalDatabaseTest {
            @RepeatedTest(2)
            @DisplayName("Must be applied without error")
            void init_without_error() {
                assertDoesNotThrow(this::liquibaseUpdate);
            }

            @Test
            @DisplayName("Set scenario sequence without holes")
            void set_scenario_sequence_value_correctly() {
                // Given
                assertDoesNotThrow(this::liquibaseUpdate);
                givenScenario();
                givenScenario();
                givenScenario();
                ScenarioEntity s1 = givenScenario();

                // When redo liquibase
                assertDoesNotThrow(this::liquibaseUpdate);

                // Then
                ScenarioEntity s2 = givenScenario();
                assertThat(s2.getId()).isEqualTo(s1.getId() + 1);

                // When redo liquibase
                assertDoesNotThrow(this::liquibaseUpdate);

                // Then
                ScenarioEntity s3 = givenScenario();
                assertThat(s3.getId()).isEqualTo(s2.getId() + 1);
            }
        }

        @Nested
        @EnableSQLiteTestInfra
        class SQLite extends AbstractLocalDatabaseTest {
            @RepeatedTest(2)
            @DisplayName("Must be applied without error")
            void init_without_error() {
                assertDoesNotThrow(this::liquibaseUpdate);
            }
        }

        @Nested
        @EnablePostgreSQLTestInfra
        class Postgres extends AbstractLocalDatabaseTest {
            @RepeatedTest(2)
            @DisplayName("Must be applied without error")
            void init_without_error() {
                assertDoesNotThrow(this::liquibaseUpdate);
            }
        }
    }
}
