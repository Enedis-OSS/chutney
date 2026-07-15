/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka.jackson;

import org.springframework.kafka.test.EmbeddedKafkaBroker;
import tools.jackson.databind.module.SimpleModule;

public class KafkaModule extends SimpleModule {

    private static final String NAME = "ChutneyKafkaModule";

    public KafkaModule() {
        super(NAME);
        addSerializer(EmbeddedKafkaBroker.class, new EmbeddedKafkaBrokerSerializer());
    }
}
