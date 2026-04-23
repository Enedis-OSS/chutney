/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.security.ApiKeyAuthentication;
import fr.enedis.chutney.server.core.domain.security.Authorization;
import fr.enedis.chutney.server.core.domain.security.Role;
import fr.enedis.chutney.server.core.domain.security.RoleNotFoundException;
import fr.enedis.chutney.server.core.domain.security.User;
import fr.enedis.chutney.server.core.domain.security.UserRoles;
import fr.enedis.chutney.tokens.domain.AccessToken;
import fr.enedis.chutney.tokens.domain.AccessTokensService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuthenticationServiceTest {

    private final Authorizations authorizations = mock(Authorizations.class);
    private final AccessTokensService accessTokensService = mock(AccessTokensService.class);
    private AuthenticationService sut;

    @BeforeEach
    public void setUp() {
        sut = new AuthenticationService(authorizations, accessTokensService);
    }

    @Test
    void get_role_from_user_id_with_write_implies_read() {
        // Given
        Role expectedRole = Role.builder()
            .withName("expectedRole")
            .withAuthorizations(List.of(Authorization.SCENARIO_READ.name(), Authorization.EXECUTION_WRITE.name()))
            .build();
        when(authorizations.read()).thenReturn(
            UserRoles.builder()
                .withRoles(List.of(expectedRole))
                .withUsers(List.of(User.builder().withId("userId").withRole(expectedRole.name).build()))
                .build()
        );

        // When
        Role role = sut.userRoleById("userId");

        // Then
        assertThat(role).isEqualTo(expectedRole);
        assertThat(role.authorizations).containsExactly(
            Authorization.SCENARIO_READ,
            Authorization.EXECUTION_WRITE,
            Authorization.EXECUTION_READ
        );
    }

    @Test
    void throw_user_not_found_when_get_role_for_authentication_for_an_unknown_user() {
        // Given
        when(authorizations.read()).thenReturn(
            UserRoles.builder().build()
        );

        // When
        assertThatThrownBy(() -> sut.userRoleById("unknown-user"))
            .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void get_api_key_authentication() {
        String token = "token";
        String user = "user";
        when(accessTokensService.userFromToken(token)).thenReturn(Optional.of(
            new AccessToken(user, "note", "hash",
            Instant.now().plus(1, ChronoUnit.DAYS))));
        String role = "role";
        when(authorizations.read()).thenReturn(
            UserRoles.builder()
                .withRoles(List.of(Role.builder()
                    .withName(role)
                    .withAuthorizations(List.of(Authorization.SCENARIO_WRITE.name()))
                    .build()))
                .withUsers(List.of(User.builder().withId(user).withRole(role).build()))
                .build()
        );

        Authentication authentication = sut.getAuthentication("token", "/path");

        assertThat(authentication).isInstanceOf(ApiKeyAuthentication.class);
        assertThat(((ApiKeyAuthentication)authentication).getAuthorities())
            .containsExactly(new SimpleGrantedAuthority(Authorization.SCENARIO_WRITE.name()),
                new SimpleGrantedAuthority(Authorization.SCENARIO_READ.name()));
    }

    @Test
    void throw_exception_when_api_key_is_wrong() {
        String token = "token";
        when(accessTokensService.userFromToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getAuthentication("X-API-KEY", "/path")).isInstanceOf(BadCredentialsException.class);
    }
}
