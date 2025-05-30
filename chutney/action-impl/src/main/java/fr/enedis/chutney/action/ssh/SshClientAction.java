/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notEmptyListValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.action.spi.validation.Validator;
import fr.enedis.chutney.action.ssh.sshj.CommandResult;
import fr.enedis.chutney.action.ssh.sshj.Commands;
import fr.enedis.chutney.action.ssh.sshj.SshClient;
import fr.enedis.chutney.action.ssh.sshj.SshJClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SshClientAction implements Action {

    private final Target target;
    private final Logger logger;
    private final List<Object> commands;
    private final String channel;

    public SshClientAction(Target target, Logger logger, @Input("commands") List<Object> commands, @Input("channel") String channel) {
        this.target = target;
        this.logger = logger;
        this.commands = commands;
        this.channel = ofNullable(channel).orElse(CHANNEL.COMMAND.name());
    }

    @Override
    public List<String> validateInputs() {
        Validator<List<Object>> commandsValidator = notEmptyListValidation(this.commands, "commands")
            .validate(Commands::from, noException -> true, "Syntax is a List of String or a List of {command: \"xxx\", timeout:\"10 s\"} Json");
        return getErrorsFrom(
            targetValidation(target),
            commandsValidator
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            Connection connection = Connection.from(target);
            Connection proxyConnection = Connection.proxyFrom(target).orElse(null);
            boolean isSshChannel = CHANNEL.SHELL.equals(CHANNEL.from(this.channel));
            SshClient sshClient = new SshJClient(connection, proxyConnection, isSshChannel, logger);

            List<CommandResult> commandResults = Commands.from(this.commands).executeWith(sshClient);

            Map<String, List<CommandResult>> actionResult = new HashMap<>();
            actionResult.put("results", commandResults);

            return ActionExecutionResult.ok(actionResult);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }

    private enum CHANNEL {
        COMMAND, SHELL;

        public static CHANNEL from(String channel) {
            for (CHANNEL value : CHANNEL.values()) {
                if (value.name().equalsIgnoreCase((channel))) {
                    return value;
                }
            }
            return COMMAND;
        }
    }
}
