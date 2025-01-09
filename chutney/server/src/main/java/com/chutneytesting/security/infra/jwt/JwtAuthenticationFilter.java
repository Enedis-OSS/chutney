/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final List<UserDetailsService> userDetailsServices;
    private final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(JwtUtil jwtUtil, List<UserDetailsService> userDetailsServices) {
        this.jwtUtil = jwtUtil;
        this.userDetailsServices = userDetailsServices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            LOGGER.info("TOKEN : " + token);
            try {
                String username = jwtUtil.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userDetails = userDetailsServices.stream()
                        .map(service -> {
                            try {
                                return service.loadUserByUsername(username);
                            } catch (Exception e) {
                                LOGGER.debug("User not found for username {}", username, e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .findFirst();
                    userDetails.ifPresent(details -> LOGGER.info("USER : " + details.getUsername() + " / " + details.getAuthorities().stream().map(Object::toString)));
                    if (userDetails.isPresent() && jwtUtil.validateToken(token, userDetails.get())) {
                        var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.get().getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while authenticating user", e);
            }
        }
        filterChain.doFilter(request, response);
    }
}
