/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.domain;

import fr.enedis.chutney.scenario.domain.gwt.GwtScenario;

public interface GwtScenarioMarshaller {

    String serialize(GwtScenario scenario);

    GwtScenario deserialize(String title, String description, String blob);

}
