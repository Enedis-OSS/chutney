/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra;

import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.security.domain.AuthenticationService;
import fr.enedis.chutney.server.core.domain.security.Authorization;
import fr.enedis.chutney.server.core.domain.security.Role;
import fr.enedis.chutney.server.core.domain.security.RoleNotFoundException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserDetailsServiceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsServiceHelper.class);

    private UserDetailsServiceHelper() {
    }

    public static UserDto grantAuthoritiesFromUserRole(UserDto userDto, AuthenticationService authenticationService) {
        UserDto dto = new UserDto(userDto);

        if (dto.getRoles().stream().anyMatch("admin"::equalsIgnoreCase)) {
            Arrays.stream(Authorization.values()).map(Authorization::name).forEach(dto::grantAuthority);
        } else {
            try {
                Role role = authenticationService.userRoleById(dto.getId());
                dto.addRole(role.name);
                role.authorizations.stream().map(Enum::name).forEach(dto::grantAuthority);
            } catch (RoleNotFoundException rnfe) {
                LOGGER.warn("User {} has no role defined", dto.getId());
                if (dto.getAuthorizations().isEmpty()) {
                    throw new NoRoleUserException(rnfe);
                }
            }
        }

        return dto;
    }
}
