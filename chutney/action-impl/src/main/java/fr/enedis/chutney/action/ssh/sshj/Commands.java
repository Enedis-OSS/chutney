/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh.sshj;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Commands {

    private static final Logger LOGGER = LoggerFactory.getLogger(Commands.class);

    public final List<Command> all;

    private Commands(List<Command> commands) {
        this.all = commands;
    }

    public static Commands from(List<Object> commands) {
        List<Command> cmds = ofNullable(commands).orElse(emptyList()).stream()
            .map(Commands::buildCommand)
            .collect(Collectors.toList());

        return new Commands(cmds);
    }

    @SuppressWarnings("unchecked")
    private static Command buildCommand(Object command) {
        return switch (command) {
            case String stringCommand -> new Command(stringCommand);
            case Map<?, ?> map -> new Command(((Map<String, String>) map).get("command"),
                ((Map<String, String>) map).get("timeout"));
            default -> throw new IllegalStateException("Unable to understand command: " + command);
        };
    }


    public List<CommandResult> executeWith(SshClient sshClient) throws IOException {
        List<CommandResult> results = new ArrayList<>();

        for (Command command : this.all) {
            LOGGER.debug("COMMANDS :: {}", command);
            CommandResult result = command.executeWith(sshClient);
            results.add(result);
            LOGGER.debug("RESULT :: {}", result);
        }

        return results;
    }

}
