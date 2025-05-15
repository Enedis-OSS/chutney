/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.jms

import fr.enedis.chutney.example.scenario.ACTIVEMQ_TARGET_NAME
import fr.enedis.chutney.example.scenario.FILMS_DESTINATION
import fr.enedis.chutney.example.scenario.activemq_scenario
import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.Environment
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.activemq.ActiveMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class JmsScenarioTest {

    private var containerJmsPort = 61616
    private var environment: ChutneyEnvironment = ChutneyEnvironment("empty")


    @Container
    // Last version for JMS 1.1 support
    val activemqContainer = ActiveMQContainer(DockerImageName.parse("apache/activemq-classic:5.19.0"))
        .withExposedPorts(containerJmsPort)

    @BeforeEach
    fun setUp() {
        val activemqAddress = activemqContainer.host
        val hostJmsPort = activemqContainer.getMappedPort(containerJmsPort)
        environment = Environment(name = "local", description = "local environment") {
            Target {
                Name(ACTIVEMQ_TARGET_NAME)
                Url("tcp://$activemqAddress:$hostJmsPort")
                Properties(
                    "java.naming.factory.initial" to "org.apache.activemq.jndi.ActiveMQSslInitialContextFactory",
                    "jndi.queue.$FILMS_DESTINATION" to FILMS_DESTINATION
                )
            }
        }
    }

    @Test
    fun `publish & consume jms message`() {
        Launcher().run(activemq_scenario(false), environment)
    }
}
