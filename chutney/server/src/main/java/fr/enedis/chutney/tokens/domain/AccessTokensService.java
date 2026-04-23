/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccessTokensService {

    private final AccessTokensRepository accessTokensRepository;
    private final AccessTokenEncoder accessTokenEncoder;

    public AccessTokensService(AccessTokensRepository accessTokensRepository,
                               AccessTokenEncoder accessTokenEncoder) {
        this.accessTokensRepository = accessTokensRepository;
        this.accessTokenEncoder = accessTokenEncoder;
    }

    public String createToken(String user, String note, Instant expiresAt) {
        String token = UUID.randomUUID().toString().replace("-", "");
        accessTokensRepository.createToken(AccessToken.create(user, token, note, expiresAt, accessTokenEncoder));
        return token;
    }

    public Optional<AccessToken> userFromToken(String token) {
        Collection<AccessToken> tokens = accessTokensRepository.getTokens();
        for (var repoToken : tokens) {
            if (repoToken.matchTokenAndIsValid(token, accessTokenEncoder)) {
                return Optional.of(repoToken);
            }
        }
        return Optional.empty();
    }
}
