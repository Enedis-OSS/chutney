/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra.jwt;

import com.chutneytesting.security.api.UserDto;
import com.chutneytesting.security.domain.AuthenticationService;
import com.chutneytesting.security.infra.UserDetailsServiceHelper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class ChutneyJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {


    private final AuthenticationService authenticationService;

    public ChutneyJwtAuthenticationConverter(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        UserDto user = new UserDto();
        user.setId(jwt.getSubject());
        user.setName(jwt.getSubject());
        user = UserDetailsServiceHelper.grantAuthoritiesFromUserRole(user, authenticationService);
        return new JwtAuthenticationToken(jwt, user.getAuthorities());
    }
}
