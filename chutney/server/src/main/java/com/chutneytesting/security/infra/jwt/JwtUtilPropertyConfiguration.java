/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("auth.jwt")
@Configuration
public class JwtUtilPropertyConfiguration {
    private String secretKey;
    private Integer tokenValidityInHours = 4;

    public String getSecretKey() {
        return secretKey;
    }

    public Integer getTokenValidityInHours() {
        return 1000 * 60 * 60 * tokenValidityInHours;
    }

    public JwtUtilPropertyConfiguration setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public JwtUtilPropertyConfiguration setTokenValidityInHours(Integer tokenValidityInHours) {
        this.tokenValidityInHours = tokenValidityInHours;
        return this;
    }
}
