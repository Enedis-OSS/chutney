/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.enedis.chutney.environment.domain.Environment;
import fr.enedis.chutney.environment.domain.EnvironmentRepository;
import fr.enedis.chutney.environment.domain.EnvironmentVariable;
import fr.enedis.chutney.environment.domain.Target;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.TargetAlreadyExistsException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import fr.enedis.chutney.tools.ThrowingConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JsonFilesEnvironmentRepositoryTest {

    private static final Path CONFIGURATION_FOLDER = Paths.get("target", "conf");

    private final EnvironmentRepository sut = new JsonFilesEnvironmentRepository(CONFIGURATION_FOLDER.toString());

    @AfterEach
    public void after() {
        try (Stream<Path> confStream = Files.list(CONFIGURATION_FOLDER)) {
            confStream.forEach(ThrowingConsumer.toUnchecked(Files::delete));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_save_configuration_then_read_it() {
        // Given
        final String url = "http://target1:8080";
        Set<EnvironmentVariable> variables = Set.of(
            new EnvironmentVariable("Key 1", "value", "TEST"),
            new EnvironmentVariable("Key 2", "other value", "TEST"));
        final Environment environment = Environment.builder()
            .withName("TEST")
            .withDescription("some description")
            .withTargets(
                Collections.singleton(
                    Target.builder()
                        .withName("target1")
                        .withEnvironment("envName")
                        .withUrl(url)
                        .build()))
            .withVariables(variables)
            .build();

        // When
        sut.save(environment);

        // Then
        Environment testEnv = sut.findByName("TEST");
        assertThat(testEnv).isNotNull();
        assertThat(testEnv.targets).containsExactly(
            Target.builder().withName("target1").withUrl(url).withEnvironment("TEST").build()
        );
        assertThat(testEnv.variables).containsExactlyInAnyOrderElementsOf(variables);
    }

    @Test
    void should_throw_exception_when_target_is_not_unique() {
        // Given
        final String url = "http://target:8080";
        final Environment environment = Environment.builder()
            .withName("TEST")
            .withDescription("some description")
            .withTargets(
                Set.of(Target.builder()
                        .withName("target0")
                        .withEnvironment("envName1")
                        .withUrl(url)
                        .build(),
                    Target.builder()
                        .withName("target1")
                        .withEnvironment("envName1")
                        .withUrl(url)
                        .build(),
                    Target.builder()
                        .withName("target1")
                        .withEnvironment("envName2")
                        .withUrl(url)
                        .build(),
                    Target.builder()
                        .withName("target2")
                        .withEnvironment("envName1")
                        .withUrl(url)
                        .build(),
                    Target.builder()
                        .withName("target2")
                        .withEnvironment("envName2")
                        .withUrl(url)
                        .build()))
            .build();
        // When & Then
        assertThatThrownBy(() -> sut.save(environment))
            .isInstanceOf(TargetAlreadyExistsException.class)
            .message()
            .isEqualTo("Targets are not unique : target2, target1");
    }

    @Test
    void should_throw_exception_when_variable_is_not_unique() {
        // Given
        final Environment environment = Environment.builder()
            .withName("TEST")
            .withDescription("some description")
            .withVariables(
                Set.of(new EnvironmentVariable("Key", "value", "TEST"),
                    new EnvironmentVariable("Key", "other value", "TEST"))
            ).build();
        // When & Then
        assertThatThrownBy(() -> sut.save(environment))
            .isInstanceOf(VariableAlreadyExistingException.class)
            .message()
            .isEqualTo("Variables are not unique : Key");
    }

    @Test
    void should_save_configuration_twice_without_creating_duplicate() {
        sut.save(Environment.builder().withName("TEST").withDescription("some description").build());
        assertThat(sut.findByName("TEST").description).isEqualTo("some description");

        sut.save(Environment.builder().withName("TEST").withDescription("some other description").build());

        assertThat(sut.listNames()).hasSize(1);
        assertThat(sut.findByName("TEST").description).isEqualTo("some other description");
    }

    @Test
    void should_list_existing_environments_names() {
        sut.save(Environment.builder().withName("TEST").withDescription("some description").build());
        assertThat(sut.listNames()).contains("TEST");

        sut.delete("TEST");
        assertThat(sut.listNames()).doesNotContain("TEST");
    }

    @Test
    void should_throws_exception_when_delete_missing_environment() {
        assertThatThrownBy(() -> sut.delete("MISSING_ENV"))
            .isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void should_throws_exception_when_find_missing_environment() {
        assertThatThrownBy(() -> sut.findByName("MISSING_ENV"))
            .isInstanceOf(EnvironmentNotFoundException.class);
    }
}
