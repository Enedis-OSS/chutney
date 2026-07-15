/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;
import java.util.Collection;
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

    public CreateTokenResult createToken(String user, String note, Instant expiresAt) {
        var token = UUID.randomUUID().toString().replace("-", "");
        var hash = accessTokenEncoder.encode(token);
        var accessToken = AccessToken.create(user, note, expiresAt, hash);
        accessTokensRepository.createToken(accessToken);
        return new CreateTokenResult(accessToken.id(), token);
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

    public Collection<AccessToken> getTokensForUser(String user) {
        return accessTokensRepository.getTokensForUser(user);
    }

    public void deleteTokenForUser(UUID id, String user) {
        List<AccessToken> tokensForUser = accessTokensRepository.getTokensForUser(user);
        Optional<AccessToken> rightToken = tokensForUser.stream()
            .filter(accessToken -> accessToken.id().equals(id) && accessToken.user().equals(user))
            .findFirst();
        if(rightToken.isEmpty()) {
            throw new TokenNotFoundException(id);
        }

        accessTokensRepository.deleteToken(rightToken.get());
    }
}
