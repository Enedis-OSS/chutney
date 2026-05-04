/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.infra;

import fr.enedis.chutney.tokens.domain.AccessTokenEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptAccessTokenEncoder implements AccessTokenEncoder {

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence token) {
        return bCryptPasswordEncoder.encode(token);
    }

    @Override
    public boolean matches(CharSequence token, String hash) {
        return bCryptPasswordEncoder.matches(token, hash);
    }
}
