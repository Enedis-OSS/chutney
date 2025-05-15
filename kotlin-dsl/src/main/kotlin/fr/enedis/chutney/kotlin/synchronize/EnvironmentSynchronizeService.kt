/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import fr.enedis.chutney.environment.domain.EnvironmentService
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingEnvironmentException
import fr.enedis.chutney.environment.infra.JsonFilesEnvironmentRepository
import fr.enedis.chutney.kotlin.execution.CHUTNEY_ENV_ROOT_PATH_DEFAULT
import fr.enedis.chutney.kotlin.synchronize.ChutneyServerServiceImpl
import fr.enedis.chutney.kotlin.util.ChutneyServerInfo

/**
 * Synchronise local environments from remote.
 */
class EnvironmentSynchronizeService(
) {


    fun synchroniseLocal(
        serverInfo: ChutneyServerInfo,
        environmentsPath: String = "$CHUTNEY_ENV_ROOT_PATH_DEFAULT/",
        force: Boolean = false
    ) {
        val environmentRepository = JsonFilesEnvironmentRepository(environmentsPath)
        val environmentService = EnvironmentService(environmentRepository)
        ChutneyServerServiceImpl.getEnvironments(serverInfo)
            .forEach {
                try {
                    environmentService.createEnvironment(it.toEnvironment(), force)
                    println("| ${it.name} local environment was synchronized")
                }
                // do nothing when environment exist and force=false
                catch (e: AlreadyExistingEnvironmentException) {
                }
            }

    }

}

