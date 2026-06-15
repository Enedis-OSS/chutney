/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.kafka.jackson.KafkaModule;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import tools.jackson.databind.json.JsonMapper;

class EmbeddedKafkaBrokerSerializerTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
        .addModule(new KafkaModule())
        .build();

    @Test
    void should_serialize_running_broker() throws Exception {
        EmbeddedKafkaKraftBroker broker = new EmbeddedKafkaKraftBroker(1, 1, "topic");
        broker.afterPropertiesSet();

        try {
            String json = objectMapper.writeValueAsString(broker);

            assertThat(json).contains("Embedded Kafka Broker");
            assertThat(json).contains(broker.getBrokersAsString());
        } finally {
            broker.destroy();
        }
    }

    @Test
    void should_serialize_broker_after_shutdown() throws Exception {
        EmbeddedKafkaKraftBroker broker = new EmbeddedKafkaKraftBroker(1, 1, "topic");
        broker.afterPropertiesSet();
        broker.destroy();

        String json = objectMapper.writeValueAsString(broker);

        assertThat(json).isEqualTo("{\"Embedded Kafka Broker\":\"shut down\"}");
    }

    @Test
    void should_serialize_broker_from_evaluated_inputs_map_after_shutdown() throws Exception {
        EmbeddedKafkaKraftBroker broker = new EmbeddedKafkaKraftBroker(1, 1, "topic");
        broker.afterPropertiesSet();
        broker.destroy();

        String json = objectMapper.writeValueAsString(Collections.singletonMap("broker", broker));

        assertThat(json).isEqualTo("{\"broker\":{\"Embedded Kafka Broker\":\"shut down\"}}");
    }
}
