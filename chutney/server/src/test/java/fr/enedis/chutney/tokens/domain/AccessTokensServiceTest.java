/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AccessTokensServiceTest {

    private final AccessTokensRepository accessTokensRepository = Mockito.mock(AccessTokensRepository.class);
    private final AccessTokensService sut = new AccessTokensService(accessTokensRepository);

    @Test
    void create_token() {
        String token = sut.crateToken("ulysse");
        assertThat(token).isNotEmpty();
        verify(accessTokensRepository).createToken();
    }
}
