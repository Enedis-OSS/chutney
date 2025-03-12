/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra.jwt;


import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("chutney.auth.jwt")
@Configuration
public class ChutneyJwtProperties {

    private String issuer;
    private Duration expiresIn;

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = Duration.ofMinutes(expiresIn);
    }

    public ChutneyJwtProperties setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String issuer() {
        return issuer;
    }

    public Duration expiresIn() {
        return expiresIn;
    }
}
