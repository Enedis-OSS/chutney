/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccessTokensService {

    private final AccessTokensRepository accessTokensRepository;

    public AccessTokensService(AccessTokensRepository accessTokensRepository) {
        this.accessTokensRepository = accessTokensRepository;
    }

    public String createToken(String user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String hash = new BCryptPasswordEncoder().encode(token);
        accessTokensRepository.createToken(new AccessToken(UUID.randomUUID().toString(), user, hash, Instant.now()));
        return token;
    }

    public Optional<AccessToken> userFromToken(String token) {
        var threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Collection<AccessToken> tokens = accessTokensRepository.getTokens();
        for (var repoToken : tokens) {
            if (encoder.matches(token, repoToken.hashedToken())
                && repoToken.createdAt().isAfter(threeMonthsAgo)) {
                return Optional.of(repoToken);
            }
        }
        return Optional.empty();
    }
}
