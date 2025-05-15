/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.domain;

import fr.enedis.chutney.server.core.domain.security.UserRoles;

public interface Authorizations {

    UserRoles read();

    void save(UserRoles userRoles);
}
