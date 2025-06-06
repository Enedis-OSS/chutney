/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.environment.domain.exception.AlreadyExistingEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import fr.enedis.chutney.environment.domain.exception.NoEnvironmentFoundException;
import fr.enedis.chutney.environment.domain.exception.SingleEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.UnresolvedEnvironmentException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvironmentServiceTest {

    EnvironmentService sut;
    EnvironmentRepository environmentRepository;

    @BeforeEach
    public void setUp() {
        environmentRepository = mock(EnvironmentRepository.class);
        sut = new EnvironmentService(environmentRepository);
    }

    @Test()
    public void create_environment_with_illegal_name_throws() {
        assertThatThrownBy(() -> sut.createEnvironment(Environment.builder().withName("illegal name").withDescription("some description").build()))
            .isInstanceOf(InvalidEnvironmentNameException.class);
    }

    @Test()
    public void update_environment_with_illegal_name_throws() {
        when(environmentRepository.findByName(any())).thenReturn(Environment.builder().withName("OLD_NAME").withDescription("some description").build());

        assertThatThrownBy(() -> sut.updateEnvironment("OLD_NAME", Environment.builder().withName("illegal name").withDescription("some description").build()))
            .isInstanceOf(InvalidEnvironmentNameException.class);
    }

    @Test
    void create_environment_should_throw_when_env_already_exist() {
        // Given
        when(environmentRepository.listNames())
            .thenReturn(List.of("EXISTING"));

        // Then
        assertThatThrownBy(() -> sut.createEnvironment(Environment.builder().withName("EXISTING").build()))
            .isInstanceOf(AlreadyExistingEnvironmentException.class);
    }

    @Test
    void create_environment_not_throw_when_env_already_exist_but_is_forced() {
        // Given
        Environment expected = Environment.builder().withName("EXISTING").build();
        when(environmentRepository.listNames())
            .thenReturn(List.of("EXISTING"));

        // When
        Environment actual = sut.createEnvironment(expected, true);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void return_the_unique_env_as_default() {
       // Given
        when(environmentRepository.listNames())
            .thenReturn(List.of("ENV"));

        // When
        String defaultEnv = sut.defaultEnvironmentName();

        // Then
        assertThat(defaultEnv).isEqualTo("ENV");

    }

    @Test
    void getting_default_env_should_throws_exception_no_env_found() {
        // Given
        when(environmentRepository.listNames())
            .thenReturn(emptyList());

        // When Then
        assertThatThrownBy(() -> sut.defaultEnvironmentName())
            .isInstanceOf(NoEnvironmentFoundException.class)
            .hasMessage("No Environment found");

    }

    @Test
    void getting_default_env_should_throws_exception_many_env_found() {
        // Given
        when(environmentRepository.listNames())
            .thenReturn(List.of("ENV", "OTHER"));

        // When Then
        assertThatThrownBy(() -> sut.defaultEnvironmentName())
            .isInstanceOf(UnresolvedEnvironmentException.class)
            .hasMessage("There is more than one environment. Could not resolve the default one");

    }

    @Test
    void delete_env_should_not_delete_the_last_env() {
        // Given
        when(environmentRepository.listNames()).thenReturn(List.of("ENV"));

        // When & Then
        assertThatThrownBy(() -> {
            sut.deleteEnvironment("ENV");
        }).isInstanceOf(SingleEnvironmentException.class)
            .hasMessageContaining("Cannot delete environment with name ENV : cannot delete the last env");
    }

    @Test
    void delete_env_should_delete_when_it_is_not_the_last_env() {
        // Given
        when(environmentRepository.listNames()).thenReturn(List.of("ENV", "OTHER"));
        doNothing().when(environmentRepository).delete(any());

        // When
        sut.deleteEnvironment("ENV");

        // Then
        verify(environmentRepository, times(1)).delete(eq("ENV"));
    }

    @Test
    void delete_variable_from_all_environments_and_update_them() {
        // Given
        String key = "API_KEY";
        List<String> envNames = List.of("dev", "prod");


        EnvironmentVariable var1Env1 = new EnvironmentVariable("API_KEY", "123", "ENV1");
        EnvironmentVariable var2Env1 = new EnvironmentVariable("TOKEN", "abc", "ENV1");
        Environment env1 = Environment.builder()
            .withName("ENV1")
            .withVariables(Set.of(var1Env1, var2Env1))
            .build();

        EnvironmentVariable var1Env2 = new EnvironmentVariable("API_KEY", "123", "ENV2");
        EnvironmentVariable var2Env2 = new EnvironmentVariable("TOKEN", "abc", "ENV2");
        Environment env2 = Environment.builder()
            .withName("ENV2")
            .withVariables(Set.of(var1Env2, var2Env2))
            .build();

        when(environmentRepository.findByNames(envNames)).thenReturn(List.of(env1, env2));

        // When
        sut.deleteVariable(key, envNames);

        // Then
        verify(environmentRepository).findByNames(envNames);
        verify(environmentRepository).save(argThat(env ->
            env.name.equals("ENV1") &&
                env.variables.size() == 1 &&
                env.variables.stream().anyMatch(v -> v.key().equals("TOKEN"))
        ));
        verify(environmentRepository).save(argThat(env ->
            env.name.equals("ENV2") &&
                env.variables.size() == 1 &&
                env.variables.stream().anyMatch(v -> v.key().equals("TOKEN"))
        ));
    }

    @Test
    void throw_exception_while_trying_to_delete_not_found_variable_from_all_environments() {
        // Given
        String key = "OTHER_KEY";
        List<String> envNames = List.of("dev", "prod");


        EnvironmentVariable var1Env1 = new EnvironmentVariable("API_KEY", "123", "ENV1");
        EnvironmentVariable var2Env1 = new EnvironmentVariable("TOKEN", "abc", "ENV1");
        Environment env1 = Environment.builder()
            .withName("ENV1")
            .withVariables(Set.of(var1Env1, var2Env1))
            .build();

        EnvironmentVariable var1Env2 = new EnvironmentVariable("API_KEY", "123", "ENV2");
        EnvironmentVariable var2Env2 = new EnvironmentVariable("TOKEN", "abc", "ENV2");
        Environment env2 = Environment.builder()
            .withName("ENV2")
            .withVariables(Set.of(var1Env2, var2Env2))
            .build();

        when(environmentRepository.findByNames(envNames)).thenReturn(List.of(env1, env2));

        // When & Then
        assertThatThrownBy(() -> sut.deleteVariable(key, envNames))
            .isInstanceOf(EnvVariableNotFoundException.class)
            .hasMessage("Variable [" + key + "] not found");
    }
}
