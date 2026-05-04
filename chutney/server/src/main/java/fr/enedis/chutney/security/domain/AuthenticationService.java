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

    /**
     * @throws InvalidApiKeyException if the api key is incorrect or expired
     */
    public ApiKeyAuthentication getAuthentication(String apiKey) {

        var accessToken = accessTokensService.accessTokenFromRaw(apiKey);
        if (accessToken.isEmpty()) {
            throw new InvalidApiKeyException();
        }

        String userName = accessToken.get().user();
        LOGGER.info("Api Key authentication success for user {}", userName);

        Set<Authorization> userAuthorizations = this.userRoleById(userName).authorizations;
        List<String> authorities = userAuthorizations
            .stream()
            .map(Enum::name)
            .collect(Collectors.toList());
        return new ApiKeyAuthentication(userName, accessToken.get().hash(), authorities);
    }
}
