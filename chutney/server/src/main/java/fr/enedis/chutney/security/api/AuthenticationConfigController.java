/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.api;

import fr.enedis.chutney.security.infra.memory.InMemoryUsersProperties;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AuthenticationConfigController.BASE_URL)
public class AuthenticationConfigController {

    public static final String BASE_URL = "/api/v1/authentication";

    private final LdapContextSource contextSource;
    private final InMemoryUsersProperties users;

    public AuthenticationConfigController(LdapContextSource contextSource, InMemoryUsersProperties users) {
        this.contextSource = contextSource;
        this.users = users;
    }

    @GetMapping(path = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthenticationConfigDto getAuthenticationConfig() {
        boolean ldapContextSourceDefined = contextSource != null;
        boolean inMemoryConfigDefined = users.getUsers() != null && !users.getUsers().isEmpty();
        return new AuthenticationConfigDto(ldapContextSourceDefined || inMemoryConfigDefined);
    }

}
