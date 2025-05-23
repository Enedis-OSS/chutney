/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.environment;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.enedis.chutney.environment.api.EnvironmentRestExceptionHandler;
import fr.enedis.chutney.environment.domain.Environment;
import fr.enedis.chutney.environment.domain.EnvironmentRepository;
import fr.enedis.chutney.environment.domain.EnvironmentService;
import fr.enedis.chutney.environment.domain.Target;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class HttpEnvironmentApiTest {

    private final String environmentBasePath = "/api/v2/environments";

    private final EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);
    private final EnvironmentService environmentService = new EnvironmentService(environmentRepository);
    private final EnvironmentApi embeddedApplication = new EmbeddedEnvironmentApi(environmentService);
    private final HttpEnvironmentApi environmentControllerV2 = new HttpEnvironmentApi(embeddedApplication);

    final Map<String, Environment> registeredEnvironments = new LinkedHashMap<>();

    private MockMvc mockMvc;

    private static Object[] params_listEnvironments_returns_all_available() {
        return new Object[]{
            new Object[]{new String[]{}},
            new Object[]{new String[]{"env1", "env2"}},
            new Object[]{new String[]{"c", "b", "a"}}
        };
    }

    @BeforeEach
    public void setUp() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        mockMvc = MockMvcBuilders.standaloneSetup(environmentControllerV2)
            .setControllerAdvice(new EnvironmentRestExceptionHandler())
            .setMessageConverters(mappingJackson2HttpMessageConverter)
            .build();
    }

    @ParameterizedTest
    @MethodSource("params_listEnvironments_returns_all_available")
    public void listEnvironments_returns_all_available(String[] environmentNames) throws Exception {
        // Given existing env and targets
        stream(environmentNames).forEach(this::addAvailableEnvironment);

        ResultActions resultActions = mockMvc.perform(get(environmentBasePath))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(environmentNames.length)));

        List<String> expectedEnvNames = stream(environmentNames)
            .sorted(Comparator.naturalOrder())
            .toList();

        for (int i = 0; i < expectedEnvNames.size(); i++) {
            resultActions.andExpect(jsonPath("$.[" + i + "].description", equalTo(expectedEnvNames.get(i) + " description")));
        }
    }

    @Test
    public void createEnvironment_adds_it_to_repository() throws Exception {
        mockMvc.perform(
                post(environmentBasePath)
                    .content("{\"name\": \"env_test\", \"description\": \"test description\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.name).isEqualTo("env_test");
        assertThat(savedEnvironment.description).isEqualTo("test description");
    }

    @Test
    public void createEnvironment_returns_400_when_name_is_invalid() throws Exception {
        doThrow(new InvalidEnvironmentNameException()).when(environmentRepository).save(any());
        mockMvc.perform(
                post(environmentBasePath)
                    .content("{\"name\": \"env test\", \"description\": \"test description\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void createEnvironment_returns_409_when_already_existing() throws Exception {
        addAvailableEnvironment("env test");

        mockMvc.perform(
                post(environmentBasePath)
                    .content("{\"name\": \"env test\", \"description\": \"test description\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isConflict());
    }

    @Test
    public void deleteEnvironment_does_not_deletes_last_env() throws Exception {
        addAvailableEnvironment("env_test");
        mockMvc.perform(delete(environmentBasePath + "/env_test"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isConflict());
    }

    @Test
    public void deleteEnvironment_returns_404_when_not_found() throws Exception {
        doThrow(new EnvironmentNotFoundException(List.of("message"))).when(environmentRepository).delete(any());

        mockMvc.perform(delete(environmentBasePath + "/env test"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteEnvironment_deletes_it_from_repo() throws Exception {
        addAvailableEnvironment("env_test_1");
        addAvailableEnvironment("env_test_2");
        mockMvc.perform(delete(environmentBasePath + "/env_test_2"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        verify(environmentRepository).delete(eq("env_test_2"));
    }

    @Test
    public void updateEnvironment_returns_404_when_not_found() throws Exception {
        when(environmentRepository.findByName(any())).thenThrow(new EnvironmentNotFoundException(List.of("message")));

        mockMvc.perform(
                put(environmentBasePath + "/env test")
                    .content("{\"name\": \"env test\", \"description\": \"test description\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateEnvironment_saves_it() throws Exception {
        addAvailableEnvironment("env_test");

        mockMvc.perform(
                put(environmentBasePath + "/env_test")
                    .content("{\"name\": \"env_test\", \"description\": \"test description\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        verify(environmentRepository, times(0)).delete(any());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.name).isEqualTo("env_test");
        assertThat(savedEnvironment.description).isEqualTo("test description");
    }

    @Test
    public void updateEnvironment_with_different_a_name_deletes_previous_one() throws Exception {
        addAvailableEnvironment("env_test");

        mockMvc.perform(
                put(environmentBasePath + "/env_test")
                    .content("{\"description\": \"test2 description\", \"name\": \"env_test_2\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        verify(environmentRepository).delete(eq("env_test"));

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.name).isEqualTo("env_test_2");
        assertThat(savedEnvironment.description).isEqualTo("test2 description");
    }


    @Test
    public void should_get_environment_when_it_exists() throws Exception {
        String[] targetNames = {"a", "b"};
        String envName = "envTest";
        addAvailableEnvironment(envName, targetNames);
        ResultActions result = mockMvc.perform(
                get(environmentBasePath + "/" + envName))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk()).andExpect(jsonPath("$.name", equalTo(envName)))
            .andExpect(jsonPath("$.description", equalTo(envName + " description")))
            .andExpect(jsonPath("$.targets.length()", equalTo(targetNames.length)));

        for (String targetName : targetNames) {
            result
                .andExpect(jsonPath("$.targets[?(@.name == '" + targetName + "')].length()",
                    equalTo(singletonList(4))))
                .andExpect(jsonPath("$.targets[?(@.name == '" + targetName + "')].properties.length()",
                    equalTo(singletonList(0))))
                .andExpect(jsonPath("$.targets[?(@.name == '" + targetName + "')].security.credential.username",
                    equalTo(emptyList())));
        }
    }

    private void addAvailableEnvironment(String envName, String... targetNames) {

        Set<Target> targets = stream(targetNames)
            .map(targetName -> Target.builder()
                .withName(targetName)
                .withEnvironment(envName)
                .withUrl("http://" + targetName.replace(' ', '_') + ":43")
                .build())
            .collect(toCollection(LinkedHashSet::new));

        registeredEnvironments.put(
            envName,
            Environment.builder()
                .withName(envName)
                .withDescription(envName + " description")
                .withTargets(targets)
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
