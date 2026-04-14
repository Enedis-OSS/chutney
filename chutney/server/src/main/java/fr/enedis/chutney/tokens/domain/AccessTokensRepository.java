/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.domain;

import java.util.Collection;

public interface AccessTokensRepository {

    void createToken(AccessToken accessToken);

    Collection<AccessToken> getTokens();
}
