/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.explore;

import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.server.core.domain.admin.Backupable;
import java.util.Optional;

public interface CurrentNetworkDescription extends Backupable {
    Optional<NetworkDescription> findCurrent();
    void switchTo(NetworkDescription networkDescription);
}
