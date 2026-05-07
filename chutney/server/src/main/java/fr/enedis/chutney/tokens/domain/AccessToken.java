/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;

public record AccessToken(String user, String note, String hash, Instant expiresAt) {

    public static AccessToken create(String user, String note, Instant expiresAt, String hash) {
        return new AccessToken(user, note, hash, expiresAt);
    }

    public boolean matchTokenAndIsValid(String token, AccessTokenEncoder encoder) {
        return encoder.matches(token, this.hash)
            && (this.expiresAt == null || this.expiresAt.isAfter(Instant.now()));
    }
}
