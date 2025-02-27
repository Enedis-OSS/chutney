/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra.sso;

import com.chutneytesting.security.api.UserDto;
import com.chutneytesting.security.domain.AuthenticationService;
import com.chutneytesting.security.infra.UserDetailsServiceHelper;
import com.chutneytesting.security.infra.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.web.filter.OncePerRequestFilter;

public class OAuth2TokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public OAuth2TokenAuthenticationFilter(JwtUtil jwtUtil, AuthenticationService authenticationService) {
        this.jwtUtil = jwtUtil;
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof OAuth2IntrospectionAuthenticatedPrincipal) {
            String username = (String) ((OAuth2IntrospectionAuthenticatedPrincipal) authentication.getPrincipal()).getAttributes().get("sub");
            UserDto user = new UserDto();
            user.setId(username);
            user.setName(username);
            user.setMail(username);
            user.setFirstname(username);
            user.setLastname(username);
            user.setRoles(Collections.emptySet());
            user = UserDetailsServiceHelper.grantAuthoritiesFromUserRole(user, authenticationService);
            Map<String, Object> claims = objectMapper.convertValue(user, Map.class);
            response.addHeader("X-Custom-Token", jwtUtil.generateToken(authentication.getName(), claims));
        }
        filterChain.doFilter(request, response);
    }
}
