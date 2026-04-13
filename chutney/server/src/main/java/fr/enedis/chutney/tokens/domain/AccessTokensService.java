/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class AccessTokensService {

    private final AccessTokensRepository accessTokensRepository;

    public AccessTokensService(AccessTokensRepository accessTokensRepository) {
        this.accessTokensRepository = accessTokensRepository;
    }

    public String createToken(String user) {
        String rawKey = UUID.randomUUID().toString().replace("-", "");
        String token = new BCryptPasswordEncoder().encode(rawKey);
        accessTokensRepository.createToken();
        return token;
    }

    public boolean matchToken(String token) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Collection<String> tokens = accessTokensRepository.getTokens();
        for (var repoToken : tokens) {
            if (encoder.matches(token, repoToken)) {
                return true;
            }
        }
        return false;
    }
}
