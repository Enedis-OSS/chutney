/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.infra;

import fr.enedis.chutney.tokens.domain.AccessToken;
import fr.enedis.chutney.tokens.domain.AccessTokensRepository;
import fr.enedis.chutney.tokens.infra.jpa.AccessTokenEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
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
    @Transactional
    public void createToken(AccessToken accessToken) {
        accessTokenJpaRepository.save(domainObjectToJpaEntity(accessToken));
    }

    @Override
    public List<AccessToken> getTokens() {
        return accessTokenJpaRepository.findAll().stream().map(jpaEntityToDomainObject()).toList();
    }

    @Override
    public List<AccessToken> getTokensForUser(String user) {
        return accessTokenJpaRepository.findByOwner(user).stream().map(jpaEntityToDomainObject()).toList();
    }

    @Override
    @Transactional
    public void deleteToken(AccessToken accessToken) {
        accessTokenJpaRepository.delete(domainObjectToJpaEntity(accessToken));
    }

    private static AccessTokenEntity domainObjectToJpaEntity(AccessToken accessToken) {
        return new AccessTokenEntity(
            accessToken.id().toString(), accessToken.user(), accessToken.note(), accessToken.hash(),
            accessToken.expiresAt() != null ? accessToken.expiresAt().toEpochMilli() : null);
    }

    private Function<AccessTokenEntity, AccessToken> jpaEntityToDomainObject() {
        return accessTokenEntity ->
            new AccessToken(UUID.fromString(accessTokenEntity.getId()), accessTokenEntity.getOwner(), accessTokenEntity.getNote(), accessTokenEntity.getHash(),
                accessTokenEntity.getExpiresAt() != null ? Instant.ofEpochMilli(accessTokenEntity.getExpiresAt()) : null);
    }
}
