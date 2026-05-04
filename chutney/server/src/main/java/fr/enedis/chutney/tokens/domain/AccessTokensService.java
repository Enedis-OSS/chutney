/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;
import java.util.List;
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
        var token = UUID.randomUUID().toString().replace("-", "");
        var hash = accessTokenEncoder.encode(token);
        accessTokensRepository.createToken(AccessToken.create(user, note, expiresAt, hash));
        return token;
    }

    public Optional<AccessToken> accessTokenFromRaw(String token) {
        List<AccessToken> tokens = accessTokensRepository.getTokens();
        for (var repoToken : tokens) {
            if (repoToken.matchTokenAndIsValid(token, accessTokenEncoder)) {
                return Optional.of(repoToken);
            }
        }
        return Optional.empty();
    }
}
