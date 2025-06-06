/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.configure;

import static fr.enedis.chutney.agent.domain.configure.ConfigurationState.EXPLORING;
import static fr.enedis.chutney.agent.domain.configure.ConfigurationState.FINISHED;
import static fr.enedis.chutney.agent.domain.configure.ConfigurationState.NOT_STARTED;
import static fr.enedis.chutney.agent.domain.configure.ConfigurationState.WRAPING_UP;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ConfigurationStateTest {
    @Test
    public void configuration_state_transition_is_coherent() {
        assertThat(NOT_STARTED.canChangeTo(EXPLORING)).isTrue();
        assertThat(EXPLORING.canChangeTo(WRAPING_UP)).isTrue();
        assertThat(WRAPING_UP.canChangeTo(FINISHED)).isTrue();

        assertThat(NOT_STARTED.canChangeTo(WRAPING_UP)).isFalse();
        assertThat(EXPLORING.canChangeTo(EXPLORING)).isFalse();
    }
}
