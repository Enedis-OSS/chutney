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

import fr.enedis.chutney.tokens.infra.BCryptAccessTokenEncoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccessTokensServiceTest {

    private final AccessTokensRepository accessTokensRepository = Mockito.mock(AccessTokensRepository.class);
    private final AccessTokensService sut = new AccessTokensService(accessTokensRepository, new BCryptAccessTokenEncoder());

    @Captor
    private final ArgumentCaptor<AccessToken> argumentCaptor = ArgumentCaptor.forClass(AccessToken.class);

    @Test
    void create_token() {
        var user = "ulysse";
        var token = sut.createToken(user, "note", Instant.now().plus(1, ChronoUnit.DAYS));

        assertThat(token).isNotEmpty();

        verify(accessTokensRepository).createToken(argumentCaptor.capture());
        AccessToken value = argumentCaptor.getValue();
        assertThat(value.user()).isEqualTo(user);
    }

    @Test
    void match_right_token() {
        var user = "bach";
        var token = sut.createToken(user, "note", Instant.now().plus(2, ChronoUnit.DAYS));
        var encoded = new BCryptPasswordEncoder().encode(token);
        AccessToken accessToken = new AccessToken(user, "note", encoded, Instant.now().plus(1, ChronoUnit.DAYS));
        when(accessTokensRepository.getTokens()).thenReturn(List.of(accessToken));
        assertThat(sut.accessTokenFromRaw(token)).contains(accessToken);
    }

    @Test
    void match_right_token_without_expired_date() {
        var user = "bach";
        var token = sut.createToken(user, "note", null);
        var encoded = new BCryptPasswordEncoder().encode(token);
        AccessToken accessToken = new AccessToken(user, "note", encoded, null);
        when(accessTokensRepository.getTokens()).thenReturn(List.of(accessToken));
        assertThat(sut.accessTokenFromRaw(token)).contains(accessToken);
    }

    @Test
    void does_not_match_wrong_token() {
        String user = "bach";
        var token = sut.createToken(user, "note", Instant.now().plus(2, ChronoUnit.DAYS));
        when(accessTokensRepository.getTokens()).thenReturn(List.of(new AccessToken(user, "note", "wrong", Instant.now().plus(1, ChronoUnit.DAYS))));
        assertThat(sut.accessTokenFromRaw(token)).isEmpty();
    }

    @Test
    void does_not_match_expired_token() {
        var user = "bach";
        var token = sut.createToken(user, "note", Instant.now());
        var encoded = new BCryptPasswordEncoder().encode(token);
        when(accessTokensRepository.getTokens()).thenReturn(List.of(new AccessToken(user, "note", encoded, Instant.now().minus(1, ChronoUnit.DAYS))));
        assertThat(sut.accessTokenFromRaw(token)).isEmpty();
    }

    @Test
    void get_tokens_for_user() {
        var user = "bach";
        AccessToken token1 = new AccessToken(user, "note1", "hash1", Instant.now().minus(1, ChronoUnit.DAYS));
        AccessToken token2 = new AccessToken(user, "note2", "hash2", Instant.now().minus(2, ChronoUnit.DAYS));
        when(accessTokensRepository.getTokensForUser(user)).thenReturn(
            List.of(token1, token2));

        Collection<AccessToken> tokensForUser = sut.getTokensForUser(user);
        assertThat(tokensForUser).hasSize(2).containsExactly(token1, token2);
    }
}
