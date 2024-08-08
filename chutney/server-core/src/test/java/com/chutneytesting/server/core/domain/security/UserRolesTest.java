/*
 * Copyright 2017-2024 Enedis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chutneytesting.server.core.domain.security;

import static com.chutneytesting.server.core.domain.security.User.userByRoleNamePredicate;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.arbitraries.SetArbitrary;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserRolesTest {

    @Test
    void minimal_build_should_be_empty() {
        UserRoles defaultBuild = UserRoles.builder().build();
        UserRoles nullBuild = UserRoles.builder().withUsers(null).withRoles(null).build();
        UserRoles emptyBuild = UserRoles.builder().withUsers(emptySet()).withRoles(emptySet()).build();

        for (UserRoles userRole : List.of(defaultBuild, nullBuild, emptyBuild)) {
            assertThat(userRole.roles()).isEmpty();
            assertThat(userRole.users()).isEmpty();
        }
    }

    @Test
    void users_must_not_have_a_blank_role() {
        User nullRoleUser = User.builder().withRole(null).build();
        User emptyRoleUser = User.builder().withRole("").build();
        User blankRoleUser = User.builder().withRole("   ").build();

        for (User user : List.of(nullRoleUser, emptyRoleUser, blankRoleUser)) {
            assertThatThrownBy(() ->
                UserRoles.builder()
                    .withUsers(singleton(user))
                    .build()
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void users_must_have_a_declared_role() {
        assertThatThrownBy(() ->
            UserRoles.builder()
                .withUsers(singleton(User.builder().withRole("UNDECLARED_ROLE").build()))
                .build()
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keep_first_user_role_when_user_has_many() {
        UserRoles sut = UserRoles.builder()
            .withRoles(List.of(
                Role.builder().withName("R").build(),
                Role.builder().withName("S").build()
            ))
            .withUsers(List.of(
                User.builder().withId("id").withRole("R").build(),
                User.builder().withId("id").withRole("S").build()
            ))
            .build();

        assertThat(sut.users())
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("id", "id")
            .hasFieldOrPropertyWithValue("roleName", "R");
    }

    @Test
    void should_find_user_by_id() {
        Role roleOfUserToFind = Role.builder().withName("roleOfUserToFind").build();
        User userToFind = User.builder().withId("userToFind").withRole(roleOfUserToFind.name).build();
        Role anotherRole = Role.builder().withName("R").build();
        User anotherUser = User.builder().withId("anotherUser").withRole(anotherRole.name).build();
        UserRoles sut = UserRoles.builder()
            .withRoles(List.of(anotherRole, roleOfUserToFind))
            .withUsers(List.of(anotherUser, userToFind))
            .build();

        assertThat(sut.userById("userToFind")).hasValue(userToFind);
        assertThat(sut.userById("unknownUser")).isEmpty();
    }

    @Test
    void should_find_users_by_role_name() {
        Role roleOfUserToFind = Role.builder().withName("roleOfUserToFind").build();
        User userToFind = User.builder().withId("userToFind").withRole(roleOfUserToFind.name).build();
        Role roleNotUsedByUser = Role.builder().withName("roleNotUsedByUser").build();
        Role anotherRole = Role.builder().withName("R").build();
        User anotherUser = User.builder().withId("anotherUser").withRole(anotherRole.name).build();
        UserRoles sut = UserRoles.builder()
            .withRoles(List.of(anotherRole, roleOfUserToFind))
            .withUsers(List.of(anotherUser, userToFind))
            .build();

        assertThat(sut.usersByRole(roleOfUserToFind)).containsExactly(userToFind);
        assertThat(sut.usersByRole(roleNotUsedByUser)).isEmpty();
    }

    @Test
    void should_find_role_by_name() {
        Role role2 = Role.builder().withName("role2").build();
        UserRoles sut = UserRoles.builder()
            .withRoles(List.of(Role.builder().withName("role1").build(), role2))
            .build();

        assertThat(sut.roleByName("role2")).isEqualTo(role2);
        assertThatThrownBy(() ->
            sut.roleByName("roleX")
        ).isInstanceOf(RoleNotFoundException.class);
    }

    @Property
    void should_keep_users_and_roles_orders_when_build(@ForAll("validRoles") Set<Role> roles) {
        Set<User> users = PropertyBasedTestingUtils.validUsers(roles);

        List<Role> orderedRoles = List.copyOf(roles);
        List<User> orderedUsers = List.copyOf(users);

        UserRoles sut = UserRoles.builder()
            .withRoles(roles)
            .withUsers(users)
            .build();

        assertThat(sut.roles()).containsExactlyElementsOf(orderedRoles);

        assertThat(Role.authorizations(List.copyOf(sut.roles())))
            .containsExactlyElementsOf(Role.authorizations(orderedRoles));

        sut.roles().forEach(role -> {
            List<User> usersForRole = orderedUsers.stream().filter(userByRoleNamePredicate(role.name)).collect(toList());
            assertThat(sut.usersByRole(role)).containsExactlyElementsOf(usersForRole);
        });
    }

    @Provide
    @SuppressWarnings("unused")
    private SetArbitrary<Role> validRoles() {
        return PropertyBasedTestingUtils.validRole().set().ofMinSize(1).ofMaxSize(10);
    }

}
