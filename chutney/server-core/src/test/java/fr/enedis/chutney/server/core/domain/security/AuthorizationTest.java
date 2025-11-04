/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AuthorizationTest {
    @Test
    void every_write_has_read() {
        Arrays.stream(Authorization.values()).forEach(auth -> {
            if (auth.isWriteAuthorization()) {
                assertThat(auth.readAuthorization()).isNotEmpty();
            }
        });
    }
}
