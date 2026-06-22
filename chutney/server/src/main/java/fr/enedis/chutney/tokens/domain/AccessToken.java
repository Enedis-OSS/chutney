/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;
import java.util.UUID;

public record AccessToken(UUID id, String user, String note, String hash, Instant expiresAt) {

    public static AccessToken create(String user, String note, Instant expiresAt, String hash) {
        var id = UUID.randomUUID();
        return new AccessToken(id, user, note, hash, expiresAt);
    }

    public boolean matchTokenAndIsValid(String token, AccessTokenEncoder encoder) {
        return encoder.matches(token, this.hash)
            && (this.expiresAt == null || this.expiresAt.isAfter(Instant.now()));
    }
}
