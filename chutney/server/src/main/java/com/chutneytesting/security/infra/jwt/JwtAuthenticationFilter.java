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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final List<UserDetailsService> userDetailsServices;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, List<UserDetailsService> userDetailsServices) {
        this.jwtUtil = jwtUtil;
        this.userDetailsServices = userDetailsServices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userDetails = userDetailsServices.stream().map(service -> {
                        try {
                            return service.loadUserByUsername(username);
                        } catch (Exception ignored) {
                            return null;
                        }
                    }).filter(Objects::nonNull).findFirst();
                    String finalToken = token;
                    userDetails.ifPresent(user -> {
                        if (jwtUtil.validateToken(finalToken, user)) {
                            var authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}
