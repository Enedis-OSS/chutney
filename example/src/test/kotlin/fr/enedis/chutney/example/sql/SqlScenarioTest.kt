/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.sql

import fr.enedis.chutney.example.scenario.SQL_TARGET_NAME
import fr.enedis.chutney.example.scenario.sql_scenario
import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.Environment
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class SqlScenarioTest {

    private var dbAddress: String = ""
    private var dbPort = 0
    private var environment: ChutneyEnvironment = ChutneyEnvironment("default value")

    @Container
    val postgresqlContainer = PostgreSQLContainer(DockerImageName.parse("postgres"))
        .withInitScript("example/sql/create_movies_table.sql")


    @BeforeEach
    fun setUp() {
        dbAddress = postgresqlContainer.host
        dbPort = postgresqlContainer.firstMappedPort
        environment = Environment(name = "local", description = "local environment") {
            Target {
                Name(SQL_TARGET_NAME)
                Url("tcp://$dbAddress:$dbPort")
                Properties(
                    "jdbcUrl" to postgresqlContainer.jdbcUrl,
                    "username" to postgresqlContainer.username,
                    "password" to postgresqlContainer.password
                )
            }
        }
    }

    @Test
    fun `insert and select movies`() {
        Launcher().run(sql_scenario, environment)
    }
}
