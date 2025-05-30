/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra.memory;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.security.domain.AuthenticationService;
import fr.enedis.chutney.security.infra.UserDetailsServiceHelper;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class InMemoryUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryUserDetailsService.class);
    private final Map<String, UserDto> users;
    private final AuthenticationService authenticationService;

    public InMemoryUserDetailsService(InMemoryUsersProperties inMemoryUsersProperties, AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        users = inMemoryUsersProperties.getUsers().stream()
            .collect(toUnmodifiableMap(UserDto::getUsername, identity()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserDto> user = Optional.ofNullable(users.get(username));
        return user
            .map(u -> UserDetailsServiceHelper.grantAuthoritiesFromUserRole(u, authenticationService))
            .orElseThrow(() -> new UsernameNotFoundException("Username not found."));
    }
}
