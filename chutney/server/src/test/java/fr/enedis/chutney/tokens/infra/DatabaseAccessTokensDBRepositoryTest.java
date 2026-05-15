/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.infra;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.tokens.domain.AccessToken;
import fr.enedis.chutney.tokens.domain.AccessTokensRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import util.infra.AbstractLocalDatabaseTest;
import util.infra.EnableH2MemTestInfra;
import util.infra.EnablePostgreSQLTestInfra;
import util.infra.EnableSQLiteTestInfra;

class DatabaseAccessTokensDBRepositoryTest {

    @Nested
    @EnableH2MemTestInfra
    class H2 extends DatabaseAccessTokensDBRepositoryTest.AllTests {
    }

    @Nested
    @EnableSQLiteTestInfra
    class SQLite extends DatabaseAccessTokensDBRepositoryTest.AllTests {
    }

    @Nested
    @EnablePostgreSQLTestInfra
    class PostgreSQL extends DatabaseAccessTokensDBRepositoryTest.AllTests {
    }

    static abstract class AllTests extends AbstractLocalDatabaseTest {

        @Autowired
        private AccessTokensRepository sut;

        @AfterEach
        void afterEach() {
            clearTables();
        }

        @Test
        void get_tokens() {
            sut.createToken(new AccessToken("pedro", "note", "hash",
                Instant.now().plus(1, ChronoUnit.HOURS)));
            List<AccessToken> tokens = sut.getTokens();
            assertThat(tokens).hasSize(1);
        }

        @Test
        void get_tokens_for_user() {
            String user = "pedro";
            sut.createToken(new AccessToken(user, "note1", "hash1",
                Instant.now().plus(1, ChronoUnit.HOURS)));
            sut.createToken(new AccessToken("pablo", "note2", "hash2",
                Instant.now().plus(2, ChronoUnit.HOURS)));
            sut.createToken(new AccessToken(user, "note3", "hash3",
                Instant.now().plus(3, ChronoUnit.HOURS)));

            List<AccessToken> tokens = sut.getTokensForUser(user);

            assertThat(tokens).hasSize(2)
                .allMatch(token -> user.equals(token.user()));
        }
    }
}
