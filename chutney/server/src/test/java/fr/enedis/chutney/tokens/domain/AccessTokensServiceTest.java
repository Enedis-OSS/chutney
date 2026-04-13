/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccessTokensServiceTest {

    private final AccessTokensRepository accessTokensRepository = Mockito.mock(AccessTokensRepository.class);
    private final AccessTokensService sut = new AccessTokensService(accessTokensRepository);

    @Test
    void create_token() {
        var token = sut.createToken("ulysse");
        assertThat(token).isNotEmpty();
        verify(accessTokensRepository).createToken();
    }

    @Test
    void match_right_token() {
        var token = sut.createToken("tokyo");
        var encoded = new BCryptPasswordEncoder().encode(token);
        when(accessTokensRepository.getTokens()).thenReturn(List.of(encoded));
        assertThat(sut.matchToken(token)).isTrue();
    }

    @Test
    void does_not_match_right_token() {
        var token = sut.createToken("tokyo");
        when(accessTokensRepository.getTokens()).thenReturn(List.of("wrong"));
        assertThat(sut.matchToken(token)).isFalse();
    }
}
