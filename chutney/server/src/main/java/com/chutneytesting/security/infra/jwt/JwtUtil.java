/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;


public class JwtUtil {


    private final ChutneyJwtProperties chutneyJwtProperties;
    private final RSAKey signinKey;
    private final JWSAlgorithm algorithm;

    public JwtUtil(ChutneyJwtProperties chutneyJwtProperties) throws JOSEException {
        this.chutneyJwtProperties = chutneyJwtProperties;
        this.signinKey = new RSAKeyGenerator(2048)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(UUID.randomUUID().toString())
            .generate();
        this.algorithm = JWSAlgorithm.RS256;
    }

    public NimbusJwtDecoder nimbusJwtDecoder() throws JOSEException {
        return NimbusJwtDecoder
            .withPublicKey(signinKey.toRSAPublicKey())
            .build();
    }

    public String generateToken(String username, Map<String, Object> claims) {
        var issuer = chutneyJwtProperties.issuer();
        var issuedAt = Instant.now();
        var expirationTime = issuedAt.plus(Duration.ofMillis(chutneyJwtProperties.expiresIn().toMillis()));
        var header = new JWSHeader(algorithm);

        JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder()
            .subject(username)
            .issuer(issuer)
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expirationTime));
        claims.forEach(claimsSetBuilder::claim);
        JWTClaimsSet claimsSet = claimsSetBuilder.build();

        var jwt = new SignedJWT(header, claimsSet);

        try {
            var signer = new RSASSASigner(signinKey);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Unable to generate JWT", e);
        }
    }
}
