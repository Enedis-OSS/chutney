/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta.jackson;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import tools.jackson.databind.module.SimpleModule;

public class ActiveMQModule extends SimpleModule {

    private static final String NAME = "ChutneyActiveMQModule";

    public ActiveMQModule() {
        super(NAME);
        addSerializer(ActiveMQServer.class, new ActiveMQServerSerializer());
    }
}
