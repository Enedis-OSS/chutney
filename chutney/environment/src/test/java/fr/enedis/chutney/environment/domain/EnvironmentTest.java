/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions .*;

@DisplayName("Environment")
public class EnvironmentTest {

    @Nested
    @DisplayName("remove variable")
    class DeleteVariable {

        @Test
        @DisplayName("from existing variable")
        void should_remove_existing_variable() {
            EnvironmentVariable var1 = new EnvironmentVariable("API_KEY", "123", "ENV");
            EnvironmentVariable var2 = new EnvironmentVariable("TOKEN", "abc", "ENV");
            Environment env = Environment.builder()
                .withVariables(Set.of(var1, var2))
                .build();

            Environment updatedEnv = env.deleteVariable("API_KEY");

            assertFalse(updatedEnv.variables.contains(var1));
            assertTrue(updatedEnv.variables.contains(var2));
            assertEquals(1, updatedEnv.variables.size());
        }

        @Test
        @DisplayName("does not remove if key does not exist")
        void should_do_nothing_if_key_does_not_exist() {
            EnvironmentVariable var1 = new EnvironmentVariable("API_KEY", "123", "ENV");
            Environment env = Environment.builder()
                .withVariables(Set.of(var1))
                .build();

            Environment updatedEnv = env.deleteVariable("NON_EXISTENT");

            assertEquals(env.variables, updatedEnv.variables);
            assertEquals(1, updatedEnv.variables.size());
        }

        @Test
        @DisplayName("from existing variable with different case")
        void should_be_case_insensitive() {
            EnvironmentVariable var1 = new EnvironmentVariable("Api_Key", "123", "ENV");
            Environment env = Environment.builder()
                .withVariables(Set.of(var1))
                .build();

            Environment updatedEnv = env.deleteVariable("api_key");

            assertFalse(updatedEnv.variables.contains(var1));
            assertTrue(updatedEnv.variables.isEmpty());
        }
    }
}

