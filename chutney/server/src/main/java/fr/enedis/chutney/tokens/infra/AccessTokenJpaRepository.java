/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.infra;

import fr.enedis.chutney.tokens.infra.jpa.AccessTokenEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessTokenJpaRepository extends JpaRepository<AccessTokenEntity,Long> {

    List<AccessTokenEntity> findByOwner(String owner);
}
