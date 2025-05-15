/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.spi.injectable;

import java.util.Map;

public interface StepDefinitionSpi {
    /**
     * Return step definition inputs.
     */
    Map<String, Object> inputs();
}
