/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security;

import fr.enedis.chutney.security.infra.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

public class HttpLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLoginSuccessHandler.class);
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public HttpLoginSuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        LOGGER.info("User {} logged in", authentication.getName());
        if (!authentication.isAuthenticated()) {
            LOGGER.debug("Authentication failure for user [{}]", authentication.getPrincipal());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            return;
        }
        LOGGER.info("User {} logged in", authentication.getName());
        Map<String, Object> claims = objectMapper.convertValue(authentication.getPrincipal(), Map.class);
        String token = jwtUtil.generateToken(authentication.getName(), claims);
        Map<String, String> tokenMap = Collections.singletonMap("token", token);
        response.setContentType("application/json");
        response.setStatus(HttpStatus.OK.value());
        objectMapper.writeValue(response.getOutputStream(), tokenMap);
    }
}
