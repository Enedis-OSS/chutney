/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.domain;

import fr.enedis.chutney.action.spi.Action;
import java.util.Collection;
import java.util.Optional;

/**
 * Registry for {@link ActionTemplate}.
 */
public interface ActionTemplateRegistry {

    /**
     * Refresh all available {@link ActionTemplate} based on given {@link ActionTemplateLoader}.<br>
     * Main use case, except for initialization, is when {@link Action} classes are added to the classpath at runtime.
     */
    void refresh();

    /**
     * @return a {@link ActionTemplate} or empty if the given type did not matched any registered {@link ActionTemplate}
     */
    Optional<ActionTemplate> getByIdentifier(String identifier);

    Collection<ActionTemplate> getAll();
}
