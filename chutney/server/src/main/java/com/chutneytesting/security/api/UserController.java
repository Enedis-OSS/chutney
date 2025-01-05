/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.api;

import com.chutneytesting.security.infra.SpringUserService;
import com.chutneytesting.security.infra.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UserController.BASE_URL)
@CrossOrigin(origins = "*")
public class UserController {

    public static final String BASE_URL = "/api/v1/user";

    private final SpringUserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);


    public UserController(SpringUserService userService, AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unused")
    public UserDto currentUser(HttpServletRequest request, HttpServletResponse response) {
        return userService.currentUser();
    }

    @PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unused")
    public UserDto loginForwardUser(HttpServletRequest request, HttpServletResponse response) {
        return this.currentUser(request, response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));
            if (authentication == null || !authentication.isAuthenticated()) {
                LOGGER.debug("Authentication failure for user [{}]", loginRequest.username());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
            }
            LOGGER.info("User {} logged in", authentication.getName());
            Map<String, Object> claims = objectMapper.convertValue(authentication.getPrincipal(), Map.class);
            String token = jwtUtil.generateToken(authentication.getName(), claims);
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
        }
    }
}
