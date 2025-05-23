/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.spi.injectable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.FinallyAction;

/**
 * Registry to declare at <i>execution-time</i> a {@link FinallyAction}.
 *
 * @see FinallyAction
 */
@FunctionalInterface
public interface FinallyActionRegistry {

    /**
     * This method is used to register a {@link FinallyAction} during the execution, as a
     * {@link Action} may have to free resources after execution.
     * <p>
     * Such registration is effective only if the execution reaches the {@link Action}
     * that defines it.
     *
     * @param finallyAction to be executed after all steps defined in a Scenario
     */
    void registerFinallyAction(FinallyAction finallyAction);
}
