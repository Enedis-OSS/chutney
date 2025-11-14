/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.target;

import static fr.enedis.chutney.environment.api.target.TargetController.TARGET_BASE_URI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import fr.enedis.chutney.environment.domain.Target;
import java.util.List;
import org.assertj.core.util.Lists;
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

public class TargetControllerTest extends TestHelper {

    private final String targetBasePath = TARGET_BASE_URI;

    private final EmbeddedTargetApi embeddedTargetApi = new EmbeddedTargetApi(environmentService);
    private final TargetController sut = new TargetController(embeddedTargetApi, environmentApi);

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        mockMvc = MockMvcBuilders.standaloneSetup(sut)
            .setControllerAdvice(new EnvironmentRestExceptionHandler())
            .setMessageConverters(mappingJackson2HttpMessageConverter)
            .build();
    }

    private static Object[] params_listTargets_returns_all_available() {
        return new Object[]{
            new Object[]{emptyList()},
            new Object[]{List.of("target1", "target2")}
        };
    }

    @ParameterizedTest
    @MethodSource("params_listTargets_returns_all_available")
    void listTargetsByEnvironments_returns_all_targets(List<String> targetNames) throws Exception {
        addAvailableEnvironment("env test", targetNames, List.of("envVar1", "envVar2"));

        ResultActions resultActions = mockMvc.perform(get(environmentBasePath + "/targets"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].name", equalTo("env test")))
            .andExpect(jsonPath("$.[0].variables", empty()));

        for (String targetName : targetNames) {
            resultActions.andExpect(jsonPath("$.[0].targets[?(@.name=='" + targetName + "')].url", contains("http://" + targetName + ":43")));
        }
    }

    @ParameterizedTest
    @MethodSource("params_listTargets_returns_all_available")
    public void listTargets_returns_all_available(List<String> targetNames) throws Exception {
        addAvailableEnvironment("env test", targetNames);

        ResultActions resultActions = mockMvc.perform(get(targetBasePath + "?environment={env}", "env test"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(targetNames.size())));

        for (int i = 0; i < targetNames.size(); i++) {
            resultActions.andExpect(jsonPath("$.[" + i + "].name", equalTo(targetNames.get(i))))
                .andExpect(jsonPath("$.[" + i + "].url", equalTo("http://" + targetNames.get(i) + ":43")));
        }
    }

    @Test
    public void list_distinct_targets_names_in_any_environment() throws Exception {
        List<String> targetsNames = Lists.list("t1", "t2", "t3");
        addAvailableEnvironment("env1", targetsNames);
        addAvailableEnvironment("env2", targetsNames);

        ResultActions resultActions = mockMvc.perform(get(targetBasePath + "/names"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));

        for (String targetName : targetsNames) {
            resultActions.andExpect(jsonPath("$[?(@=='" + targetName + "')]", hasSize(1)));
        }
    }

    @Test
    public void addTarget_saves_an_environment_with_the_new_target() throws Exception {
        addAvailableEnvironment("env_test", singletonList("server 1"));

        mockMvc.perform(
                post(targetBasePath)
                    .content("{\"name\": \"server 2\", \"url\": \"ssh://somehost:42\", \"environment\": \"env_test\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.targets).hasSize(2);
        assertThat(savedEnvironment.targets.toArray()).contains(
            Target.builder().withName("server 2").withUrl("ssh://somehost:42").withEnvironment("env_test").build()
        );
    }

    @Test
    public void addTarget_returns_409_when_already_existing() throws Exception {
        addAvailableEnvironment("env test", singletonList("server 1"));

        mockMvc.perform(
                post(targetBasePath)
                    .content("{\"name\": \"server 1\", \"url\": \"ssh://somehost:42\", \"environment\": \"env test\"}")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isConflict());
    }

    @Test
    public void deleteTarget_deletes_it_from_repo() throws Exception {
        addAvailableEnvironment("env_test", singletonList("server 1"));

        mockMvc.perform(delete(environmentBasePath + "/env_test/targets/server 1"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        verify(environmentRepository).save(eq(Environment.builder().withName("env_test").withDescription("env_test description").build()));
    }

    @Test
    public void deleteTarget_returns_404_when_not_found() throws Exception {
        addAvailableEnvironment("env_test", singletonList("server 1"));

        mockMvc.perform(delete(environmentBasePath + "/env_test/targets/server 2"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateTarget_returns_404_when_not_found() throws Exception {
        addAvailableEnvironment("env test", singletonList("server 1"));

        mockMvc.perform(
                put(targetBasePath + "/server 2")
                    .content("{\"name\": \"server 2\", \"url\": \"http://somehost2:42\" , \"environment\": \"env test\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateTarget_saves_it() throws Exception {
        addAvailableEnvironment("env_test", singletonList("server 1"));

        mockMvc.perform(
                put(targetBasePath + "/server 1")
                    .content("{\"name\": \"server 1\", \"url\": \"http://somehost2:42\", \"environment\": \"env_test\"}")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.targets).hasSize(1);
        assertThat(savedEnvironment.targets.iterator().next().url).isEqualTo("http://somehost2:42");
    }

    @Test
    public void updateTarget_with_different_name_deletes_previous_one() throws Exception {
        addAvailableEnvironment("env_test", singletonList("server 1"));

        mockMvc.perform(
                put(targetBasePath + "/server 1")
                    .content("{\"name\": \"server 2\", \"url\": \"http://somehost2:42\", \"environment\": \"env_test\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk());

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());

        Environment savedEnvironment = environmentArgumentCaptor.getValue();
        assertThat(savedEnvironment).isNotNull();
        assertThat(savedEnvironment.targets).hasSize(1);
        assertThat(savedEnvironment.targets.iterator().next().name).isEqualTo("server 2");
    }
}
