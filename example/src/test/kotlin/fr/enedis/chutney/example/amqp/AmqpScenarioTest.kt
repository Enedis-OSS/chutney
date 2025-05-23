/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.amqp

import fr.enedis.chutney.example.scenario.RABBITMQ_EXCHANGE
import fr.enedis.chutney.example.scenario.RABBITMQ_QUEUE
import fr.enedis.chutney.example.scenario.amqp_scenario
import fr.enedis.chutney.example.scenario.amqp_scenario_2
import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.Environment
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.*

@Testcontainers
class AmqpScenarioTest {

    private var rabbitAddress: String = ""
    private var rabbitPort: Int = 0
    private var environment: ChutneyEnvironment = ChutneyEnvironment("default value")

    @Container
    val rabbitmqContainer = RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"))
        .withExchange(
            RABBITMQ_EXCHANGE, "topic", false, false, true,
            Collections.emptyMap()
        )
        .withQueue(RABBITMQ_QUEUE)
        .withBinding(RABBITMQ_EXCHANGE, RABBITMQ_QUEUE, Collections.emptyMap(), "children.*", "queue")

    @BeforeEach
    fun setUp() {
        rabbitAddress = rabbitmqContainer.host
        rabbitPort = rabbitmqContainer.firstMappedPort
        environment = Environment(name = "local", description = "local environment") {
            Target {
                Name("RABBITMQ_TARGET")
                Url("amqp://$rabbitAddress:$rabbitPort")
            }
        }
    }

    @Test
    fun `publish & consume amqp message`() {
        Launcher().run(amqp_scenario, environment)
    }

    @Test
    fun `publish & get amqp message`() {
        Launcher().run(amqp_scenario_2, environment)
    }
}
