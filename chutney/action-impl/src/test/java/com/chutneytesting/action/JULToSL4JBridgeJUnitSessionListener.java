/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class JULToSL4JBridgeJUnitSessionListener implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
