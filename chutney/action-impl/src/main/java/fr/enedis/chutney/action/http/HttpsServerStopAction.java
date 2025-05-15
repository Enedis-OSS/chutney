/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http;

import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.validation.Validator;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.Objects;

public class HttpsServerStopAction implements Action {

    private Logger logger;

    private WireMockServer httpsServer;

    public HttpsServerStopAction(Logger logger, @Input("https-server") WireMockServer httpsServer) {
        this.logger = logger;
        this.httpsServer = httpsServer;
    }

    @Override
    public List<String> validateInputs() {
        Validator<WireMockServer> httpsServerValidation = of(httpsServer)
            .validate(Objects::nonNull, "No httpsServer provided");
        return getErrorsFrom(httpsServerValidation);
    }

    @Override
    public ActionExecutionResult execute() {
        logger.info("HttpsServer instance " + httpsServer + "closed");
        httpsServer.stop();
        return ActionExecutionResult.ok();
    }
}
