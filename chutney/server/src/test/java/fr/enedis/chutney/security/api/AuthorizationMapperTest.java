/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.api;

import static fr.enedis.chutney.security.PropertyBasedTestingUtils.validRights;
import static fr.enedis.chutney.security.PropertyBasedTestingUtils.validRoleName;
import static fr.enedis.chutney.security.PropertyBasedTestingUtils.validUserId;
import static net.jqwik.api.Arbitraries.just;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.security.PropertyBasedTestingUtils;
import fr.enedis.chutney.server.core.domain.security.Role;
import fr.enedis.chutney.server.core.domain.security.UserRoles;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.arbitraries.ListArbitrary;

class AuthorizationMapperTest {

    @Property
    void should_map_dto_back_and_forth(@ForAll("validDto") AuthorizationsDto dto) {
        AuthorizationsDto mapDto = AuthorizationMapper.toDto(AuthorizationMapper.fromDto(dto));
        assertThat(mapDto).isEqualTo(dto);
    }

    @Property
    void should_map_user_roles_back_and_forth(@ForAll("validUserRoles") UserRoles userRoles) {
        UserRoles mapUserRoles = AuthorizationMapper.fromDto(AuthorizationMapper.toDto(userRoles));

        assertThat(mapUserRoles).isEqualTo(userRoles);

        List<Role> mapRoles = List.copyOf(mapUserRoles.roles());
        assertThat(Role.authorizations(mapRoles))
            .containsExactlyElementsOf(Role.authorizations(List.copyOf(userRoles.roles())));

        userRoles.roles().forEach(role ->
            assertThat(mapUserRoles.usersByRole(role)).containsExactlyElementsOf(userRoles.usersByRole(role))
        );
    }

    @Provide
    @SuppressWarnings("unused")
    private Arbitrary<AuthorizationsDto> validDto() {
        return validRoleDtoList().map(ArrayList::new).flatMap(this::buildAuthorizationDto);
    }

    @Provide
    @SuppressWarnings("unused")
    private Arbitrary<UserRoles> validUserRoles() {
        return PropertyBasedTestingUtils.validUserRoles();
    }

    private ListArbitrary<AuthorizationsDto.RoleDto> validRoleDtoList() {
        return validRoleDto().list()
            .uniqueElements(AuthorizationsDto.RoleDto::getName)
            .ofMinSize(1).ofMaxSize(10);
    }

    private Arbitrary<AuthorizationsDto.RoleDto> validRoleDto() {
        return Combinators
            .combine(validRoleName(), validRights())
            .as((n, r) -> buildRoleDto(n, List.copyOf(r)));
    }

    private List<AuthorizationsDto.RoleUsersDto> validRoleUserDtoFromRoles(List<AuthorizationsDto.RoleDto> rolesDtos) {
        List<AuthorizationsDto.RoleDto> roles = new ArrayList<>(rolesDtos);
        return validUserId().set().ofSize(2 * rolesDtos.size())
            .map(ids -> {
                List<AuthorizationsDto.RoleUsersDto> roleUsersDtos = new ArrayList<>();
                Lists.partition(new ArrayList<>(ids), 2).forEach(idss -> {
                    if (!idss.isEmpty() && !roles.isEmpty()) {
                        roleUsersDtos.add(
                            buildRoleUsersDto(roles.removeFirst().getName(), idss)
                        );
                    }
                });
                return roleUsersDtos;
            }).sample();
    }

    private AuthorizationsDto.RoleDto buildRoleDto(String name, List<String> rights) {
        AuthorizationsDto.RoleDto dto = new AuthorizationsDto.RoleDto();
        dto.setName(name);
        dto.setRights(rights);
        return dto;
    }

    private AuthorizationsDto.RoleUsersDto buildRoleUsersDto(String name, List<String> users) {
        AuthorizationsDto.RoleUsersDto dto = new AuthorizationsDto.RoleUsersDto();
        dto.setName(name);
        dto.setUsers(users);
        return dto;
    }

    private Arbitrary<AuthorizationsDto> buildAuthorizationDto(List<AuthorizationsDto.RoleDto> roles) {
        List<AuthorizationsDto.RoleUsersDto> authorizations = validRoleUserDtoFromRoles(roles);
        if (!authorizations.isEmpty()) {
            AuthorizationsDto dto = new AuthorizationsDto();
            dto.setRoles(roles);
            dto.setAuthorizations(authorizations);
            return Arbitraries.of(dto);
        }
        return just(null);
    }
}
