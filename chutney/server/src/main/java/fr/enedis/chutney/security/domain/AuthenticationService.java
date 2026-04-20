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
import fr.enedis.chutney.tokens.domain.AccessTokensService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

public class AuthenticationService {

    private final Authorizations authorizations;
    private final AccessTokensService accessTokensService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";

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

    public Authentication getAuthentication(HttpServletRequest request) {
        String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
        LOGGER.info("Trying to authenticate apiKey for request {}", request.getRequestURI());

        if (apiKey == null || !accessTokensService.matchToken(apiKey)) {
            LOGGER.info("Wrong apiKey for request {}", request.getRequestURI());
            throw new BadCredentialsException("Invalid API Key");
        }

        return new ApiKeyAuthentication(AuthorityUtils.createAuthorityList(Authorization.SCENARIO_WRITE.name()));
    }
}
