/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.configure;

public interface Explorations {
    boolean changeStateToIfPossible(NetworkConfiguration networkConfiguration, ConfigurationState exploring);

    void remove(NetworkConfiguration networkConfiguration);
}
