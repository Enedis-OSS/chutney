/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.execution

import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto
import fr.enedis.chutney.engine.api.execution.TargetExecutionDto
import fr.enedis.chutney.kotlin.dsl.*

object ExecutionRequestMapper {

    fun mapScenarioToExecutionRequest(
        scenario: ChutneyScenario,
        environment: ChutneyEnvironment
    ): ExecutionRequestDto.StepDefinitionRequestDto {

        val steps = (scenario.givens + scenario.`when` + scenario.thens).filterNotNull()
        return ExecutionRequestDto.StepDefinitionRequestDto(
            scenario.title,
            null,
            null,
            "",
            mapOf(), //inputs
            mapStepDefinition(steps, environment), // steps
            mapOf(), //outputs
            mapOf() // validations
        )
    }

    private fun mapStepDefinition(
        steps: List<ChutneyStep>,
        environment: ChutneyEnvironment
    ): List<ExecutionRequestDto.StepDefinitionRequestDto> {
        return steps.map { step ->
            ExecutionRequestDto.StepDefinitionRequestDto(
                step.description,
                mapTargetToTargetExecutionDto(environment, step.implementation?.target),
                mapStrategyToStrategyExecutionDto(step.strategy),
                step.implementation?.type,
                step.implementation?.inputs,
                mapStepDefinition(step.subSteps, environment), // steps
                step.implementation?.outputs,  //outputs
                step.implementation?.validations  // validations
            )
        }
    }

    private fun mapStrategyToStrategyExecutionDto(strategy: Strategy?): ExecutionRequestDto.StepStrategyDefinitionRequestDto {
        return ExecutionRequestDto.StepStrategyDefinitionRequestDto(
            strategy?.type ?: "",
            strategy?.parameters ?: emptyMap()
        )
    }

    private fun mapTargetToTargetExecutionDto(environment: ChutneyEnvironment, target: String?): TargetExecutionDto? {
        return environment.findTarget(target)?.let {
            toTargetExecutionDto(it)
        }
    }

    private fun toTargetExecutionDto(target: ChutneyTarget): TargetExecutionDto {
        return TargetExecutionDto(
            target.name,
            target.url,
            target.properties,
            listOf()
        )
    }
}
