/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra;

import static java.util.stream.Collectors.toUnmodifiableSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.security.domain.AuthenticationService;
import fr.enedis.chutney.security.domain.CurrentUserNotFoundException;
import fr.enedis.chutney.server.core.domain.security.Authorization;
import fr.enedis.chutney.server.core.domain.security.UserService;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.stereotype.Component;

@Component
public class SpringUserService implements UserService {

    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public Set<Authorization> currentUserAuthorizations() {
        return currentUser().getAuthorizations().stream().map(Authorization::valueOf).collect(toUnmodifiableSet());
    }

    private UserDto getUserFromBearerAuthentication(Authentication authentication) {
        var principal = authentication.getPrincipal();
        if (principal instanceof UserDto) {
            return (UserDto) principal;
        }
        if (principal instanceof Jwt) {
            return getUserFromClaims(((Jwt) principal).getClaims());
        }
        if (principal instanceof OAuth2IntrospectionAuthenticatedPrincipal) {
            return getUserFromUsername(((OAuth2IntrospectionAuthenticatedPrincipal) principal).getAttributes().get("sub").toString(), new UserDto());
        }
        return null;
    }

    private UserDto getUserFromClaims(Map<String, Object> claims) {
        String username = claims.get("sub").toString();
        UserDto user = new UserDto();
        objectMapper.convertValue(claims.get("authorizations"), Set.class).forEach(authorization -> user.grantAuthority(authorization.toString()));
        return getUserFromUsername(username, user);
    }

    private UserDto getUserFromUsername(String username, UserDto user) {
        user.setId(username);
        user.setName(username);
        user.setMail(username);
        user.setFirstname(username);
        user.setLastname(username);
        user.setRoles(Collections.emptySet());
        return UserDetailsServiceHelper.grantAuthoritiesFromUserRole(user, authenticationService);
    }
}
