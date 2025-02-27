/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra;

import com.chutneytesting.security.api.UserDto;
import com.chutneytesting.security.domain.AuthenticationService;
import com.chutneytesting.security.domain.CurrentUserNotFoundException;
import com.chutneytesting.server.core.domain.security.UserService;
import java.util.Collections;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.stereotype.Component;

@Component
public class SpringUserService implements UserService {

    private final AuthenticationService authenticationService;

    SpringUserService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public UserDto currentUser() {
        final Optional<Authentication> authentication = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
        return authentication
            .map(this::getUserFromBearerAuthentication)
            .orElseThrow(CurrentUserNotFoundException::new);
    }

    @Override
    public String currentUserId() {
        return currentUser().getId();
    }

    private UserDto getUserFromBearerAuthentication(Authentication authentication) {
        var principal = authentication.getPrincipal();
        if (principal instanceof UserDto) {
            return (UserDto) principal;
        }
        if (principal instanceof OAuth2IntrospectionAuthenticatedPrincipal) {
            String username = (String) ((OAuth2IntrospectionAuthenticatedPrincipal) principal).getAttributes().get("sub");
            UserDto user = new UserDto();
            user.setId(username);
            user.setName(username);
            user.setMail(username);
            user.setFirstname(username);
            user.setLastname(username);
            user.setRoles(Collections.emptySet());
            return UserDetailsServiceHelper.grantAuthoritiesFromUserRole(user, authenticationService);
        }
        return null;
    }
}
