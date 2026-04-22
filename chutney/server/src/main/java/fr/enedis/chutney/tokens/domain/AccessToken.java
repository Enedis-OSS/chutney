/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public record AccessToken(String user, String note, String hash, Instant expiresAt) {

    public static AccessToken create(String user, String token, String note, Instant expiresAt) {
        var hash = new BCryptPasswordEncoder().encode(token);
        return new AccessToken(user, note, hash, expiresAt);
    }

    public boolean matchTokenAndIsValid(String token) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(token, this.hash) && this.expiresAt.isAfter(Instant.now());
    }
}
