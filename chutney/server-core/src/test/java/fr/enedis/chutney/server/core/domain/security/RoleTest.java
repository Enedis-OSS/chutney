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
import java.util.stream.Stream;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

class RoleTest {

    @Property
    void map_to_distinct_authorizations(@ForAll List<Authorization> authorizations) {
        Role role = Role.builder()
            .withName("role")
            .withAuthorizations(authorizations.stream().map(Enum::name).collect(toList()))
            .build();

        assertThat(role.authorizations)
            .containsExactlyInAnyOrderElementsOf(new HashSet<>(authorizations));
    }

    @Property
    void keep_authorizations_order(@ForAll List<Authorization> authorizations) {
        Role role = Role.builder()
            .withName("role")
            .withAuthorizations(authorizations.stream().map(Enum::name).collect(toList()))
            .build();

        assertThat(role.authorizations)
            .containsExactlyElementsOf(new LinkedHashSet<>(authorizations));
    }

    @Property
    void build_role(@ForAll("validRoleName") String roleName, @ForAll List<Authorization> authorizations) {
        assertThat(
            Role.builder()
                .withName(roleName)
                .withAuthorizations(authorizations.stream().map(Enum::name).collect(toList()))
                .build()
        ).isNotNull();
    }

    @Property
    void validate_role_name(@ForAll("invalidRoleName") String roleName) {
        assertThatThrownBy(() -> Role.builder().withName(roleName).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_authorization() {
        assertThatThrownBy(() -> Role.builder().withAuthorizations(singleton("UNKNOWN_AUTH")).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copy_with_write_adding_read_auth() {
        Role sut = Role.builder()
            .withName("role")
            .withAuthorizations(
                Stream.of(
                    Authorization.SCENARIO_WRITE,
                    Authorization.CAMPAIGN_WRITE,
                    Authorization.ADMIN_ACCESS,
                    Authorization.DATASET_WRITE,
                    Authorization.EXECUTION_WRITE
                ).map(Enum::name).toList()
            )
            .build();

        assertThat(sut.copyWithWriteAsRead().authorizations).containsExactlyElementsOf(List.of(
            Authorization.SCENARIO_WRITE,
            Authorization.CAMPAIGN_WRITE,
            Authorization.ADMIN_ACCESS,
            Authorization.DATASET_WRITE,
            Authorization.EXECUTION_WRITE,
            Authorization.SCENARIO_READ,
            Authorization.CAMPAIGN_READ,
            Authorization.DATASET_READ,
            Authorization.EXECUTION_READ)
        );
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
