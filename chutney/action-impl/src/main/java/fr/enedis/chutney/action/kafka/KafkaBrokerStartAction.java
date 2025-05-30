/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

public class KafkaBrokerStartAction implements Action {

    private final Logger logger;
    private final FinallyActionRegistry finallyActionRegistry;
    private final int port;
    private final List<String> topics;
    private final Map<String, String> properties;

    public KafkaBrokerStartAction(Logger logger,
                                FinallyActionRegistry finallyActionRegistry,
                                @Input("port") String port,
                                @Input("topics") List<String> topics,
                                @Input("properties") Map<String, String> properties) {
        this.logger = logger;
        this.finallyActionRegistry = finallyActionRegistry;
        this.port = ofNullable(port).map(Integer::parseInt).orElse(-1);
        this.topics = ofNullable(topics).orElse(emptyList());
        this.properties = ofNullable(properties).orElse(emptyMap());
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            EmbeddedKafkaBroker broker = new EmbeddedKafkaZKBroker(1, true, topics.toArray(new String[0]));
            configure(broker);
            logger.info("Try to start kafka broker");
            broker.afterPropertiesSet();
            createQuitFinallyAction(broker);
            return ActionExecutionResult.ok(toOutputs(broker));
        } catch (Exception e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }

    private void configure(EmbeddedKafkaBroker broker) {
        if (port > 0) {
            broker.kafkaPorts(port);
        }
        if (!properties.isEmpty()) {
            broker.brokerProperties(properties);
        }
    }

    private Map<String, Object> toOutputs(EmbeddedKafkaBroker broker) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("kafkaBroker", broker);
        return outputs;
    }

    private void createQuitFinallyAction(EmbeddedKafkaBroker broker) {
        finallyActionRegistry.registerFinallyAction(
            FinallyAction.Builder
                .forAction("kafka-broker-stop", KafkaBrokerStartAction.class)
                .withInput("broker", broker)
                .build()
        );
        logger.info("KafkaBrokerStop finally action registered");
    }
}
