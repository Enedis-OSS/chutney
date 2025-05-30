/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.infra;

import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.ActionTemplateLoader;
import fr.enedis.chutney.action.domain.ActionTemplateParser;
import fr.enedis.chutney.action.domain.ParsingError;
import fr.enedis.chutney.action.domain.ResultOrError;
import fr.enedis.chutney.tools.loader.ExtensionLoaders;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> the Action type to load as {@link ActionTemplate} using an appropriate {@link ActionTemplateParser}
 */
public class DefaultActionTemplateLoader<T> implements ActionTemplateLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultActionTemplateLoader.class);

    private final String extensionFileName;
    private final Class<T> actionInterface;
    private final ActionTemplateParser<T> actionTemplateParser;

    public DefaultActionTemplateLoader(String extensionFileName, Class<T> actionInterface, ActionTemplateParser<T> actionTemplateParser) {
        this.extensionFileName = extensionFileName;
        this.actionInterface = actionInterface;
        this.actionTemplateParser = actionTemplateParser;
    }

    @Override
    public List<ActionTemplate> load() {
        return loadClasses()
            .map(actionTemplateParser::parse)
            .peek(this::warnIfParsingError)
            .filter(ResultOrError::isOk)
            .map(parsingResult -> parsingResult.result())
            .collect(Collectors.toList());
    }

    private Stream<Class<? extends T>> loadClasses() {
        return ExtensionLoaders
            .classpathToClass("META-INF/extension/" + extensionFileName)
            .load()
            .stream()
            .peek(this::warnIfNotAction)
            .filter(this::isAction)
            .map(clazz -> (Class<? extends T>) clazz);
    }

    private void warnIfNotAction(Class<?> clazz) {
        if (!isAction(clazz)) {
            LOGGER.warn("Unable to load " + clazz.getName() + ": not a " + actionInterface.getName());
        }
    }

    private boolean isAction(Class<?> clazz) {
        return actionInterface.isAssignableFrom(clazz);
    }

    private void warnIfParsingError(ResultOrError<ActionTemplate, ParsingError> parsingResult) {
        if (parsingResult.isError()) {
            LOGGER.warn("Unable to parse Action[" + parsingResult.error().actionClass().getName() + "]: " + parsingResult.error().errorMessage());
        }
    }
}
