/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http;

import static fr.enedis.chutney.action.function.SoapFunction.soapInsertWSUsernameToken;
import static fr.enedis.chutney.action.http.HttpActionHelper.httpCommonValidation;
import static fr.enedis.chutney.action.spi.time.Duration.parseToMs;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.http.domain.HttpAction;
import fr.enedis.chutney.action.http.domain.HttpClient;
import fr.enedis.chutney.action.http.domain.HttpClientFactory;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class HttpSoapAction implements Action {

    private static final String DEFAULT_TIMEOUT = "2000 ms";

    private final Target target;
    private final Logger logger;
    private final String uri;
    private final String username;
    private final String password;
    private final Object body;
    private final String timeout;
    private final Map<String, String> headers;

    public HttpSoapAction(Target target, Logger logger,
                        @Input("uri") String uri,
                        @Input("body") String body,
                        @Input("username") String username,
                        @Input("password") String password,
                        @Input("timeout") String timeout,
                        @Input("headers") Map<String, String> headers) {
        this.target = target;
        this.logger = logger;
        this.uri = uri;
        this.body = body;
        this.username = username;
        this.password = password;
        this.timeout = ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
        this.headers = headers != null ? headers : new HashMap<>();
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            httpCommonValidation(target, timeout)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        HttpClient httpClient = new HttpClientFactory().create(logger, target, String.class, (int) parseToMs(timeout));
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::add);
        Object envelope = soapInsertWSUsernameToken(this.username, this.password, body.toString());
        Supplier<ResponseEntity<String>> caller = () -> httpClient.post(this.uri, ofNullable(envelope).orElse("{}"), httpHeaders);
        return HttpAction.httpCall(logger, caller);
    }
}
