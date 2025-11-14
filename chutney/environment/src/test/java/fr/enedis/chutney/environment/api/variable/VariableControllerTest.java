/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.variable;

import static fr.enedis.chutney.environment.api.variable.EnvironmentVariableController.VARIABLE_BASE_URI;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enedis.chutney.environment.api.EnvironmentRestExceptionHandler;
import fr.enedis.chutney.environment.api.TestHelper;
import fr.enedis.chutney.environment.domain.Environment;
import fr.enedis.chutney.environment.domain.EnvironmentVariable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class VariableControllerTest extends TestHelper {

    private final String variablesBasePath = VARIABLE_BASE_URI;

    private final EmbeddedVariableApi embeddedVariableApi = new EmbeddedVariableApi(environmentService);
    private final EnvironmentVariableController sut = new EnvironmentVariableController(embeddedVariableApi, environmentApi);

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
        mockMvc = MockMvcBuilders.standaloneSetup(sut)
            .setControllerAdvice(new EnvironmentRestExceptionHandler())
            .setMessageConverters(mappingJackson2HttpMessageConverter)
            .build();
    }

    @Test
    void listVariablesByEnvironments_list_all_variables() throws Exception {
        List<String> envVars = List.of("envVar1", "envVar2");
        addAvailableEnvironment("env test", List.of("target1", "target2"), envVars);

        ResultActions resultActions = mockMvc.perform(get(environmentBasePath + "/variables"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].name", equalTo("env test")))
            .andExpect(jsonPath("$.[0].targets", empty()));

        for (String envVar : envVars) {
            resultActions.andExpect(jsonPath("$.[0].variables[?(@.key=='" + envVar + "' && @.env=='env test')].value", contains(envVar)));
        }
    }

    @Test
    void add_new_variable_on_a_given_env() throws Exception {
        addAvailableEnvironment("env_test", emptyList(), emptyList());
        mockMvc.perform(
                post(variablesBasePath)
                    .content(oneVariableBody.formatted("key", "value", "env_test"))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.variables).hasSize(1);
        assertThat(savedEnvironment.variables.toArray()).contains(
            new EnvironmentVariable("key", "value", "env_test")
        );
    }


    @Test
    void returns_409_when_new_variable_already_exist() throws Exception {
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
    void delete_variable_from_given_env() throws Exception {
        String key = "key";
        addAvailableEnvironment("test", emptyList(), List.of(key));
        when(environmentRepository.findByNames(List.of("test")))
            .thenReturn(List.of(registeredEnvironments.get("test")));

        mockMvc.perform(put(variablesBasePath + "/" + key)
                .content(oneVariableBody.formatted(key, "", "test"))
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        verify(environmentRepository)
            .save(eq(Environment.builder().withName("test").withDescription("test description").build()));
    }

    @Test
    void delete_variable_from_all_envs() throws Exception {
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

        verify(environmentRepository).save(
            eq(Environment.builder().withName("test").withDescription("test description").build()));
        verify(environmentRepository).save(
            eq(Environment.builder().withName("prod").withDescription("prod description").build()));
    }

    @Test
    void returns_404_when_deleting_unknown_variable() throws Exception {
        addAvailableEnvironment("env_test", emptyList(), List.of("key1"));
        when(environmentRepository.findByNames(List.of("env_test")))
            .thenReturn(List.of(registeredEnvironments.get("env_test")));
        mockMvc.perform(delete(variablesBasePath + "/key2"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isNotFound());
    }

    @Test
    void update_should_create_variable_on_given_env() throws Exception {
        addAvailableEnvironment("test", emptyList(), List.of("key1"));

        mockMvc.perform(
                put(variablesBasePath + "/key1")
                    .content(oneVariableBody.formatted("key1", "value", "test"))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.variables).hasSize(1);
        assertThat(savedEnvironment.variables.toArray()).contains(
            new EnvironmentVariable("key1", "value", "test")
        );
    }

    @Test
    void update_variable_name_without_duplicating() throws Exception {
        addAvailableEnvironment("test", emptyList(), List.of("key1"));

        mockMvc.perform(
                put(variablesBasePath + "/key1")
                    .content(oneVariableBody.formatted("key1", "value", "test"))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.variables).hasSize(1);
        EnvironmentVariable next = savedEnvironment.variables.iterator().next();
        assertThat(next.key()).isEqualTo("key1");
        assertThat(next.value()).isEqualTo("value");
    }
}
