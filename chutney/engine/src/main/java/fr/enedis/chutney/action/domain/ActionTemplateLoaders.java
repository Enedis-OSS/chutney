/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.domain;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A Collection of loaders, allow to get all {@link ActionTemplate} in order, respecting each {@link ActionTemplateLoader} precedence.
 */
public class ActionTemplateLoaders {

    private final List<ActionTemplateLoader> loaders;

    public ActionTemplateLoaders(List<ActionTemplateLoader> loaders) {
        this.loaders = loaders;
    }

    List<ActionTemplate> orderedTemplates() {
        return loaders
            .stream()
            .flatMap(delegate -> delegate.load().stream())
            .collect(Collectors.toList());
    }
}
