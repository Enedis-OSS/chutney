/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.api;

import fr.enedis.chutney.security.infra.memory.InMemoryUsersProperties;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@RestController
@RequestMapping(AuthenticationConfigController.BASE_URL)
public class AuthenticationConfigController {

    public static final String BASE_URL = "/api/v1/authentication";

    private final Optional<LdapContextSource> contextSource;
    private final Optional<InMemoryUsersProperties> users;

    public AuthenticationConfigController(Optional<LdapContextSource> contextSource, Optional<InMemoryUsersProperties> users) {
        this.contextSource = contextSource;
        this.users = users;
    }

    @GetMapping(path = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthenticationConfigDto getAuthenticationConfig() {
        boolean ldapContextSourceDefined = contextSource.isPresent();
        boolean inMemoryConfigDefined = users.map(u -> u.getUsers() != null && !users.get().getUsers().isEmpty()).orElse(false);
        return new AuthenticationConfigDto(ldapContextSourceDefined || inMemoryConfigDefined);
    }

}
