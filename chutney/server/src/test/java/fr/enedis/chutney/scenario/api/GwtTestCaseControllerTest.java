/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.enedis.chutney.scenario.domain.gwt.GwtScenario;
import fr.enedis.chutney.scenario.domain.gwt.GwtStep;
import fr.enedis.chutney.scenario.domain.gwt.GwtStepImplementation;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.security.infra.SpringUserService;
import fr.enedis.chutney.server.core.domain.scenario.AggregatedRepository;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class GwtTestCaseControllerTest {

    private MockMvc mockMvc;
    private final AggregatedRepository<GwtTestCase> testCaseRepository = mock(AggregatedRepository.class);
    private final SpringUserService userService = mock(SpringUserService.class);
    private final UserDto currentUser = new UserDto();

    @BeforeEach
    public void setUp() {
        currentUser.setId("currentUser");
        when(userService.currentUser()).thenReturn(currentUser);
        GwtTestCaseController testCaseController = new GwtTestCaseController(testCaseRepository, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(testCaseController).build();

        // Default stubbing
        when(testCaseRepository.save(any(GwtTestCase.class))).thenReturn("1");
    }

    @Test
    public void save_should_return_uri_of_the_scenario() throws Exception {

        URL resource = this.getClass().getResource("/raw_testcases/testcase-without-tech-step.json");
        String exampleWithoutTechStep = new String(Files.readAllBytes(Paths.get(resource.toURI())));

        AtomicReference<String> bodyHolder = new AtomicReference<>();

        // Save a scenario
        mockMvc.perform(post("/api/scenario/v2")
            .contentType(APPLICATION_JSON)
            .content(exampleWithoutTechStep))
            .andDo(result -> bodyHolder.set(result.getResponse().getContentAsString()))
            .andExpect(status().isOk());

        ArgumentCaptor<GwtTestCase> scenario = ArgumentCaptor.forClass(GwtTestCase.class);
        verify(testCaseRepository).save(scenario.capture());


        assertThat(bodyHolder.get()).isEqualTo("1");
        assertThat(scenario.getValue().metadata.title).isEqualTo("__titre__");
        assertThat(scenario.getValue().metadata.description).isEqualTo("__description__");
        assertThat(scenario.getValue().metadata.tags).containsExactly("TAG1", "TAG2");
        GwtTestCase expected = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder()
                .withTitle("__titre__")
                .withDescription("__description__")
                .build())
            .withScenario(GwtScenario.builder()
                .withGivens(Arrays.asList(
                    GwtStep.builder().withDescription("given 1").withSubSteps(
                        GwtStep.builder().withDescription("given sub step 1.1").build(),
                        GwtStep.builder().withDescription("given sub step 1.2").build()).build()
                    ,
                    GwtStep.builder().withDescription("given 2").build(),
                    GwtStep.builder().withDescription("given 3").build()
                    )
                )
                .withWhen(GwtStep.builder().withDescription("when 1").build())
                .withThens(Arrays.asList(
                    GwtStep.builder().withDescription("then 1").build(),
                    GwtStep.builder().withDescription("then 2").build(),
                    GwtStep.builder().withDescription("then 3").build()
                    )
                ).build())
            .build();

        assertThat(scenario.getValue().scenario).isEqualTo(expected.scenario);
    }

    @Test
    public void save_example_with_tech_step_should_return_uri_of_the_scenario() throws Exception {

        URL resource = this.getClass().getResource("/raw_testcases/testcase-with-tech-step.json");
        String contents = new String(Files.readAllBytes(Paths.get(resource.toURI())));

        AtomicReference<String> bodyHolder = new AtomicReference<>();

        // Save a scenario
        mockMvc.perform(
            post("/api/scenario/v2")
                .content(contents)
                .contentType(APPLICATION_JSON)
        )
            //.andDo(MockMvcResultHandlers.print())
            .andDo(result -> bodyHolder.set(result.getResponse().getContentAsString()))
            .andExpect(status().isOk());

        ArgumentCaptor<GwtTestCase> actual = ArgumentCaptor.forClass(GwtTestCase.class);
        verify(testCaseRepository).save(actual.capture());

        assertThat(bodyHolder.get()).isEqualTo("1");
        assertThat(actual.getValue().metadata.title).isEqualTo("__titre__");
        assertThat(actual.getValue().metadata.description).isEqualTo("__description__");

        GwtTestCase expected = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder()
                .withTitle("__titre__")
                .withDescription("__description__")
                .build())
            .withScenario(GwtScenario.builder()
                .withWhen(
                    GwtStep.builder().withDescription("when 3 executable steps").withSubSteps(
                        GwtStep.builder().withDescription("executable step in json")
                            .withImplementation(new GwtStepImplementation("debug", "", null, null, null, null)).build(),
                        GwtStep.builder().withDescription("executable step with trailing spaces & without root braces")
                            .withImplementation(new GwtStepImplementation("debug", "", null, null, null, null)).build(),
                        GwtStep.builder().withDescription("executable step in hjson with comment & no quotes")
                            .withImplementation(new GwtStepImplementation("debug", "", null, null, null, null)).build()).build()
                )
                .withThens(Arrays.asList(
                    GwtStep.builder().withDescription("then one executable step").withSubSteps(
                        GwtStep.builder().withDescription("executable step with trailing spaces, root braces & \\r")
                            .withImplementation(new GwtStepImplementation("debug", "", null, null, null, null)).build()).build(),

                    GwtStep.builder().withDescription("then another executable step").withSubSteps(
                        GwtStep.builder().withDescription("executable step with docString ''' ")
                            .withImplementation(new GwtStepImplementation(
                                "sql",
                                "COCO",
                                Collections.singletonMap("statements", Arrays.asList("DELETE FROM COCO WHERE ID = 'MOMO'", "DELETE FROM CUCU WHERE ID = 'MIMI'", "INSERT INTO CECE ('ID') VALUES ('MEME')")), null, null, null)).build()).build()
                    )
                ).build())
            .build();

        assertThat(actual.getValue().scenario).isEqualTo(expected.scenario);
    }
}
