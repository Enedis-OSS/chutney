/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security;

import fr.enedis.chutney.security.api.UserDto;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String userName;
    private final String hashedToken;

    public ApiKeyAuthentication(String userName, String hashedToken, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userName = userName;
        this.hashedToken = hashedToken;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return hashedToken;
    }

    @Override
    public Object getPrincipal() {
        UserDto user = new UserDto();
        user.setId(userName);
        user.setName(userName);
        user.setRoles(getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        return user;
    }
}
