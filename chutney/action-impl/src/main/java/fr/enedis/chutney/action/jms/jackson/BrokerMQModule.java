/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms.jackson;

import tools.jackson.databind.module.SimpleModule;
import org.apache.activemq.broker.BrokerService;

public class BrokerMQModule extends SimpleModule {

    private static final String NAME = "ChutneyBrokerMQModule ";

    public BrokerMQModule() {
        super(NAME);
        addSerializer(BrokerService.class, new BrokerServiceSerializer());
    }
}
