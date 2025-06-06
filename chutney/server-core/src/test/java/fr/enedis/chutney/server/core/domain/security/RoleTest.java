/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.security;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

class RoleTest {

    @Property
    public void should_map_to_distinct_authorizations(@ForAll List<Authorization> authorizations) {
        Role role = Role.builder()
            .withName("role")
            .withAuthorizations(authorizations.stream().map(Enum::name).collect(toList()))
            .build();

        assertThat(role.authorizations)
            .containsExactlyInAnyOrderElementsOf(new HashSet<>(authorizations));
    }

    @Property
    public void should_keep_authorizations_order(@ForAll List<Authorization> authorizations) {
        Role role = Role.builder()
            .withName("role")
            .withAuthorizations(authorizations.stream().map(Enum::name).collect(toList()))
            .build();

        assertThat(role.authorizations)
            .containsExactlyElementsOf(new LinkedHashSet<>(authorizations));
    }

    @Property
    public void should_build_role(@ForAll("validRoleName") String roleName, @ForAll List<Authorization> authorizations) {
        assertThat(
            Role.builder()
                .withName(roleName)
                .withAuthorizations(authorizations.stream().map(Enum::name).collect(toList()))
                .build()
        ).isNotNull();
    }

    @Property
    public void should_validate_role_name(@ForAll("invalidRoleName") String roleName) {
        assertThatThrownBy(() -> Role.builder().withName(roleName).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_validate_authorization() {
        assertThatThrownBy(() -> Role.builder().withAuthorizations(singleton("UNKNOWN_AUTH")).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Provide
    @SuppressWarnings("unused")
    private Arbitrary<String> validRoleName() {
        return PropertyBasedTestingUtils.validRoleName();
    }

    @Provide
    @SuppressWarnings("unused")
    private Arbitrary<String> invalidRoleName() {
        return PropertyBasedTestingUtils.invalidRoleName();
    }
}
