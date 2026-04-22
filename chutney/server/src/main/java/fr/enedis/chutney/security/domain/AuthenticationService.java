/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.domain;

import fr.enedis.chutney.security.ApiKeyAuthentication;
import fr.enedis.chutney.server.core.domain.security.Authorization;
import fr.enedis.chutney.server.core.domain.security.Role;
import fr.enedis.chutney.server.core.domain.security.RoleNotFoundException;
import fr.enedis.chutney.server.core.domain.security.User;
import fr.enedis.chutney.server.core.domain.security.UserRoles;
import fr.enedis.chutney.tokens.domain.AccessToken;
import fr.enedis.chutney.tokens.domain.AccessTokensService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

public class AuthenticationService {

    private final Authorizations authorizations;
    private final AccessTokensService accessTokensService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(Authorizations authorizations,
                                 AccessTokensService accessTokensService) {
        this.authorizations = authorizations;
        this.accessTokensService = accessTokensService;
    }

    public Role userRoleById(String userId) {
        UserRoles userRoles = authorizations.read();
        User user = userRoles.userById(userId)
            .orElseThrow(() -> RoleNotFoundException.forUser(userId));
        return userRoles.roleByName(user.roleName);
    }

    public Authentication getAuthentication(String apiKey, String requestURI) {

        Optional<AccessToken> user = accessTokensService.userFromToken(apiKey);
        if (user.isEmpty()) {
            LOGGER.info("Wrong Api-key for request {}", requestURI);
            throw new BadCredentialsException("Invalid API Key");
        }

        LOGGER.info("Api-key authentication success for user {} and for request {}", user.get(), requestURI);

        return new ApiKeyAuthentication(user.get().user(), user.get().hashedToken(), AuthorityUtils.createAuthorityList(Authorization.SCENARIO_WRITE.name()));
    }
}
