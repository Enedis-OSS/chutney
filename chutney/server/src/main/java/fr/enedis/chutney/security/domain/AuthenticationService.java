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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

        var user = accessTokensService.accessTokenFromRaw(apiKey);
        if (user.isEmpty()) {
            LOGGER.info("Wrong Api Key for request {}", requestURI);
            throw new BadCredentialsException("Invalid API Key");
        }

        String userName = user.get().user();
        LOGGER.info("Api Key authentication success for user {} and for request {}", userName, requestURI);

        Set<Authorization> userAuthorizations = this.userRoleById(userName).authorizations;
        List<String> authorities = userAuthorizations
            .stream()
            .map(Enum::name)
            .collect(Collectors.toList());
        return new ApiKeyAuthentication(userName, user.get().hash(),
            AuthorityUtils.createAuthorityList(authorities));
    }
}
