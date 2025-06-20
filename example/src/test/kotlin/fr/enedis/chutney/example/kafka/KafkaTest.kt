/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.kafka

import fr.enedis.chutney.example.scenario.kafka_scenario
import fr.enedis.chutney.kotlin.dsl.Environment
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName


class KafkaTest {
    private val kafkaContainer = ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))

    @BeforeEach
    fun setUp() {
        kafkaContainer.start()
    }

    @AfterEach
    fun tearDown() {
        kafkaContainer.stop()
    }

    @Test
    fun `publish & consume kafka message`() {
        val env = Environment("Global", "") {
            Target {
                Name("target")
                Url("tcp://${kafkaContainer.bootstrapServers}")
                Properties("auto.offset.reset" to "earliest")
            }
        }

        Launcher().run(kafka_scenario, env)
    }
}
