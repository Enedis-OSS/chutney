/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config.web;

import fr.enedis.chutney.admin.domain.BackupNotFoundException;
import fr.enedis.chutney.campaign.domain.CampaignNotFoundException;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingTargetException;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import fr.enedis.chutney.environment.domain.exception.SingleEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.TargetNotFoundException;
import fr.enedis.chutney.execution.domain.campaign.CampaignAlreadyRunningException;
import fr.enedis.chutney.execution.domain.campaign.CampaignEmptyExecutionException;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionNotFoundException;
import fr.enedis.chutney.security.domain.CurrentUserNotFoundException;
import fr.enedis.chutney.server.core.domain.dataset.DataSetAlreadyExistException;
import fr.enedis.chutney.server.core.domain.dataset.DataSetNotFoundException;
import fr.enedis.chutney.server.core.domain.execution.FailedExecutionAttempt;
import fr.enedis.chutney.server.core.domain.execution.RunningScenarioExecutionDeleteException;
import fr.enedis.chutney.server.core.domain.execution.ScenarioConversionException;
import fr.enedis.chutney.server.core.domain.execution.ScenarioNotRunningException;
import fr.enedis.chutney.server.core.domain.execution.report.ReportNotFoundException;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import fr.enedis.chutney.server.core.domain.scenario.AlreadyExistingScenarioException;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotParsableException;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);
    private final ChutneyMetrics metrics;

    public RestExceptionHandler(ChutneyMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        LOGGER.warn(ex.getMessage());
        metrics.onHttpError(status);
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        LOGGER.warn(ex.getMessage());
        metrics.onHttpError(status);
        return super.handleHttpMessageNotWritable(ex, headers, status, request);
    }

    @ExceptionHandler({
        FailedExecutionAttempt.class,
        RuntimeException.class
    })
    public ResponseEntity<Object> _500(RuntimeException ex, WebRequest request) {
        return handleExceptionInternalWithExceptionMessageAsBody(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({
        BackupNotFoundException.class,
        CampaignExecutionNotFoundException.class,
        CampaignNotFoundException.class,
        CurrentUserNotFoundException.class,
        DataSetNotFoundException.class,
        EnvironmentNotFoundException.class,
        ReportNotFoundException.class,
        ScenarioNotFoundException.class,
        ScenarioNotRunningException.class,
        TargetNotFoundException.class,
        EnvVariableNotFoundException.class,
        DataSetNotFoundException.class
    })
    protected ResponseEntity<Object> notFound(RuntimeException ex, WebRequest request) {
        return handleExceptionInternalWithExceptionMessageAsBody(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({
        AlreadyExistingEnvironmentException.class,
        AlreadyExistingScenarioException.class,
        AlreadyExistingTargetException.class,
        CampaignAlreadyRunningException.class,
        DataSetAlreadyExistException.class,
        SingleEnvironmentException.class,
        CampaignEmptyExecutionException.class,
        RunningScenarioExecutionDeleteException.class
    })
    protected ResponseEntity<Object> conflict(RuntimeException ex, WebRequest request) {
        return handleExceptionInternalWithExceptionMessageAsBody(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({
        ScenarioConversionException.class,
        ScenarioNotParsableException.class
    })
    protected ResponseEntity<Object> unprocessableEntity(RuntimeException ex, WebRequest request) {
        return handleExceptionInternalWithExceptionMessageAsBody(ex, HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler({
        DateTimeParseException.class,
        HttpMessageConversionException.class,
        InvalidEnvironmentNameException.class,
        IllegalArgumentException.class
    })
    protected ResponseEntity<Object> badRequest(RuntimeException ex, WebRequest request) {
        return handleExceptionInternalWithExceptionMessageAsBody(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({
        AccessDeniedException.class
    })
    protected ResponseEntity<Object> forbidden(RuntimeException ex, WebRequest request) {
        return handleExceptionInternalWithExceptionMessageAsBody(ex, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({
        UnsupportedOperationException.class
    })
    protected ResponseEntity<Object> notImplemented(RuntimeException ex, WebRequest request) {
        return handleExceptionInternalWithExceptionMessageAsBody(ex, HttpStatus.NOT_IMPLEMENTED, request);
    }

    private ResponseEntity<Object> handleExceptionInternalWithExceptionMessageAsBody(RuntimeException ex, HttpStatus status, WebRequest request) {
        logException(ex, status);
        metrics.onHttpError(status);
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), status, request);
    }

    private void logException(RuntimeException ex, HttpStatus status) {
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            LOGGER.error(status.name(), ex);
        } else {
            LOGGER.warn("{} >> {}", status.name(), ex.getMessage());
        }
    }
}
