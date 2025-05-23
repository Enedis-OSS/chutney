/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Failure;
import static fr.enedis.chutney.action.ssh.fakes.FakeServerSsh.buildLocalSshServer;
import static fr.enedis.chutney.action.ssh.fakes.FakeTargetInfo.buildTargetWithPassword;
import static fr.enedis.chutney.action.ssh.fakes.FakeTargetInfo.buildTargetWithPrivateKeyWithPassphrase;
import static fr.enedis.chutney.action.ssh.fakes.FakeTargetInfo.buildTargetWithPrivateKeyWithoutPassphrase;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.action.ssh.fakes.FakeServerSsh;
import fr.enedis.chutney.action.ssh.sshj.Command;
import fr.enedis.chutney.action.ssh.sshj.CommandResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.server.SshServer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SshClientActionTest {

    private static SshServer fakeSshServer;

    @BeforeAll
    public static void prepare_ssh_server() throws Exception {
        fakeSshServer = buildLocalSshServer();
        fakeSshServer.start();
    }

    @AfterAll
    public static void stop_ssh_server() throws Exception {
        fakeSshServer.stop();
    }

    @Test
    public void should_succeed_to_execute_a_command_with_password() {
        // Given
        Target targetMock = buildTargetWithPassword(fakeSshServer);
        TestLogger logger = new TestLogger();
        List<CommandResult> expectedResults = new ArrayList<>();
        expectedResults.add(new CommandResult(new Command("echo Hello"), 0, "Hello\n", ""));

        List<Object> commands = singletonList("echo Hello");

        // When
        SshClientAction action = new SshClientAction(targetMock, logger, commands, null);
        ActionExecutionResult actualResult = action.execute();

        // Then
        assertThat(actualResult.outputs.get("results")).usingRecursiveComparison().isEqualTo(expectedResults);
        assertThat(logger.info.getFirst()).startsWith("Authentication via username/password as ");
    }

    @ParameterizedTest
    @MethodSource("usernamePrivateKeyTargets")
    public void should_succeed_to_execute_a_command_with_private_key(Target targetMock) {
        // Given
        TestLogger logger = new TestLogger();
        List<CommandResult> expectedResults = new ArrayList<>();
        expectedResults.add(new CommandResult(new Command("echo Hello"), 0, "Hello\n", ""));

        String command = "echo Hello";

        // When
        SshClientAction action = new SshClientAction(targetMock, logger, singletonList(command), null);
        ActionExecutionResult actualResult = action.execute();

        // Then
        assertThat(actualResult.outputs.get("results")).usingRecursiveComparison().isEqualTo(expectedResults);
        assertThat(logger.info.getFirst()).startsWith("Authentication via private key as ");
    }

    public static List<Arguments> usernamePrivateKeyTargets() {
        return List.of(
            Arguments.of(buildTargetWithPrivateKeyWithoutPassphrase(fakeSshServer)),
            Arguments.of(buildTargetWithPrivateKeyWithPassphrase(fakeSshServer))
        );
    }

    @Test
    public void should_succeed_with_timed_out_result() {
        // Given
        Logger logger = mock(Logger.class);
        Target target = buildTargetWithPassword(fakeSshServer);

        Map<String, String> command = new HashMap<>();
        command.put("command", OsUtils.isWin32() ? "START /WAIT TIMEOUT /T 1 /NOBREAK >NUL" : "sleep 1s");
        command.put("timeout", "1 ms");

        // When
        SshClientAction sshClient = new SshClientAction(target, logger, singletonList(command), null);
        ActionExecutionResult actualResult = sshClient.execute();

        // Then
        assertThat(actualResult.status).isEqualTo(Failure);
    }

    @Test
    public void should_fail_when_target_server_is_not_responding() throws IOException {
        // Given
        Logger logger = mock(Logger.class);
        fakeSshServer.stop();
        Target target = buildTargetWithPassword(fakeSshServer);
        String command = "echo Hello";

        // when
        SshClientAction sshClient = new SshClientAction(target, logger, singletonList(command), null);
        ActionExecutionResult actualResult = sshClient.execute();

        // Then
        assertThat(actualResult.status).isEqualTo(Failure);
    }

    @Test
    void should_validate_all_input() {
        SshClientAction sshClientAction = new SshClientAction(null, null, null, null);
        List<String> errors = sshClientAction.validateInputs();

        assertThat(errors.size()).isEqualTo(6);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(errors.getFirst()).isEqualTo("No target provided");
        softly.assertThat(errors.get(1)).isEqualTo("[Target name is blank] not applied because of exception java.lang.NullPointerException(null)");
        softly.assertThat(errors.get(2)).isEqualTo("[Target url is not valid: null target] not applied because of exception java.lang.NullPointerException(null)");
        softly.assertThat(errors.get(3)).isEqualTo("[Target url has an undefined host: null target] not applied because of exception java.lang.NullPointerException(null)");
        softly.assertThat(errors.get(4)).isEqualTo("No commands provided (List)");
        softly.assertThat(errors.get(5)).isEqualTo("[commands should not be empty] not applied because of exception java.lang.NullPointerException(Cannot invoke \"java.util.List.isEmpty()\" because \"m\" is null)");
        softly.assertAll();
    }

    @Test
    public void should_succeed_to_execute_a_command_with_password_via_proxy() throws IOException {
        // Given
        SshServer proxy = FakeServerSsh.buildLocalProxy("proxySshUser", "proxySshPassword");
        String proxyUrl = "ssh://" + proxy.getHost() + ":" + proxy.getPort();
        try {
            proxy.start();

            Target targetMock = buildTargetWithPassword(fakeSshServer, proxyUrl, "proxySshUser", "proxySshPassword");
            TestLogger logger = new TestLogger();
            List<CommandResult> expectedResults = new ArrayList<>();
            expectedResults.add(new CommandResult(new Command("echo Hello"), 0, "Hello\n", ""));

            List<Object> commands = singletonList("echo Hello");

            // When
            SshClientAction action = new SshClientAction(targetMock, logger, commands, null);
            ActionExecutionResult actualResult = action.execute();

            // Then
            assertThat(actualResult.outputs.get("results")).usingRecursiveComparison().isEqualTo(expectedResults);
            assertThat(logger.info.getFirst()).startsWith("Authentication via username/password as proxySshUser");
            assertThat(logger.info.get(1)).startsWith("Authentication via username/password as mockssh");
        } finally {
            proxy.stop();
        }
    }
}
