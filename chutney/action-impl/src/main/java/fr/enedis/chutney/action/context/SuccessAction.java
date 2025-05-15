/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.context;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;

public class SuccessAction implements Action {

    public SuccessAction() {
    }

    @Override
    public ActionExecutionResult execute() {
        return ActionExecutionResult.ok();
    }
}
