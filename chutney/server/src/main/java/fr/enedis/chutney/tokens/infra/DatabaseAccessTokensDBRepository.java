/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.infra;

import fr.enedis.chutney.tokens.domain.AccessToken;
import fr.enedis.chutney.tokens.domain.AccessTokensRepository;
import fr.enedis.chutney.tokens.infra.jpa.AccessTokenEntity;
import java.time.Instant;
import java.util.Collection;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DatabaseAccessTokensDBRepository implements AccessTokensRepository {

    private final AccessTokenJpaRepository accessTokenJpaRepository;

    public DatabaseAccessTokensDBRepository(AccessTokenJpaRepository accessTokenJpaRepository) {
        this.accessTokenJpaRepository = accessTokenJpaRepository;
    }

    @Override
    public void createToken(AccessToken accessToken) {
        accessTokenJpaRepository.save(new AccessTokenEntity(
            accessToken.user(), accessToken.note(), accessToken.hash(), accessToken.expiresAt().toEpochMilli()));
    }

    @Override
    public Collection<AccessToken> getTokens() {
        return accessTokenJpaRepository.findAll().stream().map(accessTokenEntity ->
            new AccessToken(accessTokenEntity.getOwner(), accessTokenEntity.getNote(), accessTokenEntity.getHash(),
                Instant.ofEpochMilli(accessTokenEntity.getExpiresAt()))).toList();
    }
}
