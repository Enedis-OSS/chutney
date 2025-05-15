/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution;

import fr.enedis.chutney.action.TestActionTemplateFactory.FailAction;
import fr.enedis.chutney.action.TestActionTemplateFactory.SuccessAction;
import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.ActionTemplateLoader;
import fr.enedis.chutney.action.domain.ActionTemplateParserV2;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal {@link ActionTemplateLoader} with simple actions:
 * <ul>
 * <li>{@link SuccessAction}</li>
 * <li>{@link FailAction}</li>
 * </ul>
 */
public class TestActionTemplateLoader implements ActionTemplateLoader {

    private final List<ActionTemplate> actionTemplates = new ArrayList<>();

    public TestActionTemplateLoader() {
        this.actionTemplates.add(new ActionTemplateParserV2().parse(SuccessAction.class).result());
        this.actionTemplates.add(new ActionTemplateParserV2().parse(FailAction.class).result());
    }

    @Override
    public List<ActionTemplate> load() {
        return actionTemplates;
    }
}
