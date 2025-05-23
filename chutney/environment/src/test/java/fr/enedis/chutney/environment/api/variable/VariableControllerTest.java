/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.variable;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.enedis.chutney.environment.api.EnvironmentRestExceptionHandler;
import fr.enedis.chutney.environment.domain.Environment;
import fr.enedis.chutney.environment.domain.EnvironmentRepository;
import fr.enedis.chutney.environment.domain.EnvironmentService;
import fr.enedis.chutney.environment.domain.EnvironmentVariable;
import fr.enedis.chutney.environment.domain.Target;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class VariableControllerTest {

    private final String variablesBasePath = "/api/v2/variables";

    private final EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);
    private final EnvironmentService environmentService = new EnvironmentService(environmentRepository);
    private final EmbeddedVariableApi embeddedApplication = new EmbeddedVariableApi(environmentService);
    private final EnvironmentVariableController targetController = new EnvironmentVariableController(embeddedApplication);

    final Map<String, Environment> registeredEnvironments = new LinkedHashMap<>();

    private MockMvc mockMvc;
    private final String variableTemplate = """
            {
                "key": "%s",
                "value": "%s",
                "env":"%s"
            }
        """;

    private final String oneVariableBody = """
            [
                %s
            ]
        """.formatted(variableTemplate);

    @BeforeEach
    public void setUp() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        mockMvc = MockMvcBuilders.standaloneSetup(targetController)
            .setControllerAdvice(new EnvironmentRestExceptionHandler())
            .setMessageConverters(mappingJackson2HttpMessageConverter)
            .build();
    }

    @Test
    public void should_add_new_variable_on_a_given_env() throws Exception {
        addAvailableEnvironment("env_test", emptyList(), emptyList());
        mockMvc.perform(
                post(variablesBasePath)
                    .content(oneVariableBody.formatted("key", "value", "env_test"))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository, times(1)).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.variables).hasSize(1);
        assertThat(savedEnvironment.variables.toArray()).contains(
            new EnvironmentVariable("key", "value", "env_test")
        );
    }


    @Test
    public void should_returns_409_when_new_variable_already_exist() throws Exception {
        addAvailableEnvironment("env test", emptyList(), List.of("key"));

        mockMvc.perform(
                post(variablesBasePath)
                    .content(oneVariableBody.formatted("key", "value", "env test"))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isConflict());
    }

    @Test
    public void should_delete_variable_from_given_env() throws Exception {
        String key = "key";
        addAvailableEnvironment("test", emptyList(), List.of(key));
        when(environmentRepository.findByNames(List.of("test")))
            .thenReturn(List.of(registeredEnvironments.get("test")));

        mockMvc.perform(put(variablesBasePath + "/" + key)
                .content(oneVariableBody.formatted(key, "", "test"))
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        verify(environmentRepository, times(1))
            .save(eq(Environment.builder().withName("test").withDescription("test description").build()));
    }

    @Test
    public void should_delete_variable_from_all_envs() throws Exception {
        String key = "key";
        addAvailableEnvironment("test", emptyList(), List.of(key));
        addAvailableEnvironment("prod", emptyList(), List.of(key));
        when(environmentRepository.findByNames(List.of("test", "prod")))
            .thenReturn(List.of(registeredEnvironments.get("test"), registeredEnvironments.get("prod")));
        when(environmentRepository.findByName("test"))
            .thenReturn(registeredEnvironments.get("test"));
        when(environmentRepository.findByName("prod"))
            .thenReturn(registeredEnvironments.get("prod"));

        mockMvc.perform(delete(variablesBasePath + "/" + key))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        verify(environmentRepository, times(1)).save(
            eq(Environment.builder().withName("test").withDescription("test description").build()));
        verify(environmentRepository, times(1)).save(
            eq(Environment.builder().withName("prod").withDescription("prod description").build()));
    }

    @Test
    public void should_returns_404_when_deleting_unknown_variable() throws Exception {
        addAvailableEnvironment("env_test", emptyList(), List.of("key1"));
        when(environmentRepository.findByNames(List.of("env_test")))
            .thenReturn(List.of(registeredEnvironments.get("env_test")));
        mockMvc.perform(delete(variablesBasePath + "/key2"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isNotFound());
    }

    @Test
    public void update_should_create_variable_on_given_env() throws Exception {
        addAvailableEnvironment("test", emptyList(), List.of("key1"));

        mockMvc.perform(
                put(variablesBasePath + "/key1")
                    .content(oneVariableBody.formatted("key1", "value", "test"))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository, times(1)).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.variables).hasSize(1);
        assertThat(savedEnvironment.variables.toArray()).contains(
            new EnvironmentVariable("key1", "value", "test")
        );
    }

    @Test
    public void should_update_variable_name_without_duplicating() throws Exception {
        addAvailableEnvironment("test", emptyList(), List.of("key1"));

        mockMvc.perform(
                put(variablesBasePath + "/key1")
                    .content(oneVariableBody.formatted("key1", "value", "test"))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository, times(1)).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.variables).hasSize(1);
        EnvironmentVariable next = savedEnvironment.variables.iterator().next();
        assertThat(next.key()).isEqualTo("key1");
        assertThat(next.value()).isEqualTo("value");
    }

    private void addAvailableEnvironment(String envName, List<String> targetsNames, List<String> variablesNames) {

        Set<Target> targets = targetsNames.stream()
            .map(targetName -> Target.builder()
                .withName(targetName)
                .withEnvironment(envName)
                .withUrl("http://" + targetName.replace(' ', '_') + ":43")
                .build())
            .collect(toCollection(LinkedHashSet::new));

        Set<EnvironmentVariable> variables = variablesNames.stream()
            .map(name -> new EnvironmentVariable(name, name, envName)).collect(toCollection(LinkedHashSet::new));


        registeredEnvironments.put(
            envName,
            Environment.builder()
                .withName(envName)
                .withDescription(envName + " description")
                .withTargets(targets)
                .withVariables(variables)
                .build()
        );

        when(environmentRepository.findByName(eq(envName)))
            .thenAnswer(iom -> {
                    String envNameParam = iom.getArgument(0);
                    if (!registeredEnvironments.containsKey(envNameParam)) {
                        throw new EnvironmentNotFoundException(List.of("test env not found"));
                    }
                    return registeredEnvironments.get(envNameParam);
                }
            );

        when(environmentRepository.listNames())
            .thenReturn(new ArrayList<>(registeredEnvironments.keySet()));
    }
}
