/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.security;

import java.util.Set;

public interface UserService {
    String currentUserId();

    Set<Authorization> currentUserAuthorizations();
}
