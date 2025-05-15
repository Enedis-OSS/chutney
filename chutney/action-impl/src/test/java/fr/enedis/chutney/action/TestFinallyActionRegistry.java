/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action;

import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;
import java.util.ArrayList;
import java.util.List;

public class TestFinallyActionRegistry implements FinallyActionRegistry {

    public final List<FinallyAction> finallyActions = new ArrayList<>();

    @Override
    public void registerFinallyAction(FinallyAction finallyAction) {
        finallyActions.add(finallyAction);
    }
}
