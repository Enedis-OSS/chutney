/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.enedis.chutney.environment.domain.exception.AlreadyExistingTargetException;
import fr.enedis.chutney.scenario.api.GwtTestCaseController;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.server.core.domain.execution.ScenarioConversionException;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import fr.enedis.chutney.server.core.domain.scenario.AggregatedRepository;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RestExceptionHandlerTest {

    private MockMvc mockMvc;
    private AggregatedRepository<GwtTestCase> testCaseRepository = mock(AggregatedRepository.class);
    private ChutneyMetrics mockedMetrics = mock(ChutneyMetrics.class);

    @BeforeEach
    public void setup() {
        GwtTestCaseController testCaseController = new GwtTestCaseController(testCaseRepository, null);

        mockMvc = MockMvcBuilders
            .standaloneSetup(testCaseController)
            .setControllerAdvice(new RestExceptionHandler(mockedMetrics)).build();
    }

    public static List<Arguments> usernamePrivateKeyTargets() {
        return List.of(
            of(new ScenarioNotFoundException("12345"), NOT_FOUND, status().isNotFound()),
            of(new HttpMessageConversionException(""), BAD_REQUEST, status().isBadRequest()),
            of(new IllegalArgumentException(), BAD_REQUEST, status().isBadRequest()),
            of(new AlreadyExistingTargetException("", ""), CONFLICT, status().isConflict()),
            of(new ScenarioConversionException("", mock(Exception.class)), UNPROCESSABLE_ENTITY, status().isUnprocessableEntity())
        );
    }

    @ParameterizedTest
    @MethodSource("usernamePrivateKeyTargets")
    void should_return_corresponding_http_error_status(RuntimeException exception, HttpStatus status, ResultMatcher statusMatcher) throws Exception {
        // Given
        when(testCaseRepository.findById("12345")).thenThrow(exception);

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/api/scenario/v2/12345")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(statusMatcher);

        //Then
        verify(mockedMetrics).onHttpError(eq(status));
    }
}
