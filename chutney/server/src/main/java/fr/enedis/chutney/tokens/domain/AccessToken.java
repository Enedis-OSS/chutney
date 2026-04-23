/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;

public record AccessToken(String user, String note, String hash, Instant expiresAt) {

    public static AccessToken create(String user, String token, String note, Instant expiresAt, AccessTokenEncoder encoder) {
        var hash = encoder.encode(token);
        return new AccessToken(user, note, hash, expiresAt);
    }

    public boolean matchTokenAndIsValid(String token, AccessTokenEncoder encoder) {
        return encoder.matches(token, this.hash) && this.expiresAt.isAfter(Instant.now());
    }
}
