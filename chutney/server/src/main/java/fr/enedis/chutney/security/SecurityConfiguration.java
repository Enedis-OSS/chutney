/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security;

import fr.enedis.chutney.security.domain.AuthenticationService;
import fr.enedis.chutney.security.domain.Authorizations;
import fr.enedis.chutney.tokens.domain.AccessTokensService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfiguration {

    @Bean
    public AuthenticationService authenticationService(Authorizations authorizations,
                                                       AccessTokensService accessTokensService) {
        return new AuthenticationService(authorizations, accessTokensService);
    }
}
