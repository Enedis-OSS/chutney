/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra;

import fr.enedis.chutney.server.core.domain.security.RoleNotFoundException;
import org.springframework.security.authentication.AccountStatusException;

public class NoRoleUserException extends AccountStatusException {

    public NoRoleUserException(RoleNotFoundException rnfe) {
        super(rnfe.getMessage());
    }
}
