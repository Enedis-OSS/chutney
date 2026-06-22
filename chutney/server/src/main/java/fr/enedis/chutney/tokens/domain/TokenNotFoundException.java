/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.util.UUID;

public class TokenNotFoundException extends RuntimeException {

    public TokenNotFoundException(UUID id) {
        super("Token [" + id + "] not found for user !");
    }

}
