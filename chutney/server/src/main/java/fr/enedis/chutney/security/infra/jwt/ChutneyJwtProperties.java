/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra.jwt;


import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("chutney.auth.jwt")
@Configuration
public class ChutneyJwtProperties {

    private String issuer = "chutney";
    private long expiresIn = 240;

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String issuer() {
        return issuer;
    }

    public Duration expiresIn() {
        return Duration.ofMinutes(expiresIn);
    }
}
