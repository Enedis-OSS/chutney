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
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import util.infra.AbstractLocalDatabaseTest;
import util.infra.EnableH2MemTestInfra;

@EnableH2MemTestInfra
class DatabaseAccessTokensDBRepositoryTest extends AbstractLocalDatabaseTest {

    @Autowired
    private AccessTokensRepository sut;

    @Test
    void get_tokens() {
        sut.createToken(new AccessToken("pedro", "note", "87654",
            Instant.now().plus(1, ChronoUnit.HOURS)));
        Collection<AccessToken> tokens = sut.getTokens();
        assertThat(tokens).hasSize(1);
    }
}
