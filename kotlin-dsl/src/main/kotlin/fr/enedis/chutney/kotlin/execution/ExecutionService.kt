/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.execution

import fr.enedis.chutney.ExecutionConfiguration
import fr.enedis.chutney.engine.api.execution.DatasetDto
import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto
import fr.enedis.chutney.environment.EnvironmentConfiguration
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto
import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.ChutneyScenario
import fr.enedis.chutney.kotlin.dsl.ChutneyTarget

const val CHUTNEY_ROOT_PATH_DEFAULT = ".chutney"
const val CHUTNEY_ENV_ROOT_PATH_DEFAULT = "$CHUTNEY_ROOT_PATH_DEFAULT/environments"


class ExecutionService(
    environmentJsonRootPath: String = CHUTNEY_ENV_ROOT_PATH_DEFAULT
) {

    private val executionConfiguration = ExecutionConfiguration()
    private val embeddedEnvironmentApi = EnvironmentConfiguration(environmentJsonRootPath).embeddedEnvironmentApi


    fun execute(
        scenario: ChutneyScenario,
        environment: ChutneyEnvironment,
        constants: Map<String, String> = emptyMap(),
        dataset: List<Map<String,String>> = emptyList()
    ): Long {
        val datasetDto = DatasetDto(constants, dataset);
        return executionConfiguration.embeddedTestEngine()
            .executeAsync(
                ExecutionRequestDto(
                    ExecutionRequestMapper.mapScenarioToExecutionRequest(scenario, environment),
                    fr.enedis.chutney.engine.api.execution.EnvironmentDto(environment.name, environment.variables),
                    datasetDto
                )
            )
    }

    fun execute(
        scenario: ChutneyScenario,
        environmentName: String? = null,
        constants: Map<String, String> = emptyMap(),
        dataset: List<Map<String,String>> = emptyList()
    ): Long {
        return execute(scenario, getEnvironment(environmentName), constants, dataset)
    }

    fun waitLastReport(executionId: Long): StepExecutionReportDto {
        return executionConfiguration.embeddedTestEngine()
            .receiveNotification(executionId)
            .blockingLast()
    }

    fun getEnvironment(environmentName: String? = null): ChutneyEnvironment {
        val executionEnv = environmentName.takeUnless { it.isNullOrBlank() } ?: embeddedEnvironmentApi.defaultEnvironmentName()
        val environmentDto = embeddedEnvironmentApi.getEnvironment(executionEnv)
        return mapEnvironmentNameToChutneyEnvironment(environmentDto)
    }

    private fun mapEnvironmentNameToChutneyEnvironment(environmentDto: EnvironmentDto): ChutneyEnvironment {
        return ChutneyEnvironment(
            name = environmentDto.name,
            description = environmentDto.description,
            targets = environmentDto.targets.map { targetDto ->
                ChutneyTarget(
                    name = targetDto.name,
                    url = targetDto.url,
                    properties = targetDto.propertiesToMap()
                )
            }
        )
    }
}
