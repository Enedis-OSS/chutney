/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.api.execution;

import static fr.enedis.chutney.engine.api.execution.HttpTestEngine.EXECUTION_URL;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReportBuilder;
import fr.enedis.chutney.tools.Jsons;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class HttpTestEngineTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpTestEngineTest.class);

    @Test
    public void controller_maps_anemic_request_and_call_engine() throws Exception {
        TestEngine engine = mock(TestEngine.class);
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(new ObjectMapper().findAndRegisterModules());

        MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new HttpTestEngine(engine))
            .setMessageConverters(mappingJackson2HttpMessageConverter)
            .build();

        StepExecutionReport report = new StepExecutionReportBuilder()
            .setName("test")
            .setEnvironment("environment")
            .setDuration(2L)
            .setStartDate(Instant.now())
            .setStatus(Status.SUCCESS)
            .setType("actionType")
            .setStrategy("strategy")
            .setTargetName("targetName")
            .setTargetUrl("targetUrl")
            .createStepExecutionReport();

        when(engine.execute(any()))
            .thenReturn(StepExecutionReportMapper.toDto(report));

        ExecutionRequestDto executionRequestDto = Jsons.loadJsonFromClasspath("scenarios_examples/simpleStep/scenario_sample_1.json", ExecutionRequestDto.class);
        String body = Jsons.objectMapper().writeValueAsString(executionRequestDto);

        mvc
            .perform(MockMvcRequestBuilders
                .post(EXECUTION_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(body)
            )
            .andDo(result -> LOGGER.info(result.getResponse().getContentAsString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("test")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.environment", CoreMatchers.equalTo("environment")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.duration", CoreMatchers.equalTo(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.equalTo(Status.SUCCESS.name())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo("actionType")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.strategy", CoreMatchers.equalTo("strategy")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.targetName", CoreMatchers.equalTo("targetName")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.targetUrl", CoreMatchers.equalTo("targetUrl")))
        ;

        verify(engine, times(1)).execute(any());
    }

    @Test
    public void method_should_not_be_implemented_for_remote() {
        TestEngine engine = mock(TestEngine.class);
        HttpTestEngine httpTestEngine = new HttpTestEngine(engine);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> httpTestEngine.executeAsync(null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> httpTestEngine.pauseExecution(null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> httpTestEngine.resumeExecution(null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> httpTestEngine.stopExecution(null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> httpTestEngine.receiveNotification(null));
    }
}
