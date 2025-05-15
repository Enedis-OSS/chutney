/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.durationValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.ssh.SshClientFactory.DEFAULT_TIMEOUT;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.action.spi.time.Duration;
import fr.enedis.chutney.action.ssh.sftp.ChutneySftpClient;
import fr.enedis.chutney.action.ssh.sftp.SftpClientImpl;
import java.util.List;

public class SftpUploadAction implements Action {

    private final Target target;
    private final Logger logger;
    private final String source;
    private final String destination;
    private final String timeout;

    public SftpUploadAction(Target target, Logger logger, @Input("source") String source, @Input("destination") String destination, @Input("timeout") String timeout) {
        this.target = target;
        this.logger = logger;
        this.source = source;
        this.destination = destination;
        this.timeout = defaultIfEmpty(timeout, DEFAULT_TIMEOUT);
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(source, "source"),
            notBlankStringValidation(destination, "destination"),
            durationValidation(timeout, "timeout"),
            targetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try (ChutneySftpClient client = SftpClientImpl.buildFor(target, Duration.parseToMs(timeout), logger)) {
            client.upload(source, destination);
            return ActionExecutionResult.ok();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }

}

