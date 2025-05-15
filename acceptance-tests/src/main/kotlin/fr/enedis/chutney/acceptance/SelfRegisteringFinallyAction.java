/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;

/**
 * Action registering itself as {@link FinallyAction}.
 * <p>
 * Used in a scenario, this action proves that there is no infinite-loop when a {@link FinallyAction} registers another
 * {@link FinallyAction} with the same identifier
 */
public class SelfRegisteringFinallyAction implements Action {

  private final FinallyActionRegistry finallyActionRegistry;

  public SelfRegisteringFinallyAction(FinallyActionRegistry finallyActionRegistry) {
    this.finallyActionRegistry = finallyActionRegistry;
  }

  @Override
  public ActionExecutionResult execute() {
    finallyActionRegistry.registerFinallyAction(FinallyAction.Builder.forAction("self-registering-finally", SelfRegisteringFinallyAction.class).build());
    return ActionExecutionResult.ok();
  }
}

