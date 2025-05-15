/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.delegation;


import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;

public interface DelegationClient {

    StepExecutionReport handDown(Step stepDefinition, NamedHostAndPort delegate) throws CannotDelegateException;

}
