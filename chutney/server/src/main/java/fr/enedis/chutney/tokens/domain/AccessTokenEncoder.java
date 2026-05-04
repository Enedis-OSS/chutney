/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

public interface AccessTokenEncoder {

    String encode(CharSequence token);
    boolean matches(CharSequence token, String hash);
}
