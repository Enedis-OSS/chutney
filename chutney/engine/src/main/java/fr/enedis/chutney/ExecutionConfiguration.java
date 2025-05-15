/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import static fr.enedis.chutney.tools.Streams.identity;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import fr.enedis.chutney.action.domain.ActionTemplateLoader;
import fr.enedis.chutney.action.domain.ActionTemplateLoaders;
import fr.enedis.chutney.action.domain.ActionTemplateParserV2;
import fr.enedis.chutney.action.domain.ActionTemplateRegistry;
import fr.enedis.chutney.action.domain.DefaultActionTemplateRegistry;
import fr.enedis.chutney.action.infra.DefaultActionTemplateLoader;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.injectable.ActionsConfiguration;
import fr.enedis.chutney.engine.api.execution.EmbeddedTestEngine;
import fr.enedis.chutney.engine.api.execution.TestEngine;
import fr.enedis.chutney.engine.domain.delegation.DelegationService;
import fr.enedis.chutney.engine.domain.execution.ExecutionEngine;
import fr.enedis.chutney.engine.domain.execution.ExecutionManager;
import fr.enedis.chutney.engine.domain.execution.engine.DefaultExecutionEngine;
import fr.enedis.chutney.engine.domain.execution.engine.DefaultStepExecutor;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.StepDataEvaluator;
import fr.enedis.chutney.engine.domain.execution.evaluation.SpelFunctionCallback;
import fr.enedis.chutney.engine.domain.execution.evaluation.SpelFunctions;
import fr.enedis.chutney.engine.domain.execution.strategies.StepExecutionStrategies;
import fr.enedis.chutney.engine.domain.execution.strategies.StepExecutionStrategy;
import fr.enedis.chutney.engine.domain.report.Reporter;
import fr.enedis.chutney.engine.infrastructure.delegation.HttpClient;
import fr.enedis.chutney.tools.ThrowingFunction;
import fr.enedis.chutney.tools.loader.ExtensionLoaders;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

public class ExecutionConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionConfiguration.class);

    private final ActionTemplateRegistry actionTemplateRegistry;
    private final Reporter reporter;
    private final ExecutionEngine executionEngine;
    private final TestEngine embeddedTestEngine;

    private final SpelFunctions spelFunctions;
    private final Set<StepExecutionStrategy> stepExecutionStrategies;
    private final Long reporterTTL;

    public ExecutionConfiguration() {
        this(5L, Executors.newFixedThreadPool(10), emptyMap(), null, null);
    }

    public ExecutionConfiguration(Long reporterTTL, ExecutorService actionExecutor, Map<String, String> actionsConfiguration, String user, String password) {
        this.reporterTTL = reporterTTL;

        ActionTemplateLoader actionTemplateLoaderV2 = createActionTemplateLoaderV2();
        spelFunctions = createSpelFunctions();
        stepExecutionStrategies = createStepExecutionStrategies();

        actionTemplateRegistry = new DefaultActionTemplateRegistry(new ActionTemplateLoaders(singletonList(actionTemplateLoaderV2)));
        reporter = createReporter();
        executionEngine = createExecutionEngine(actionExecutor, user, password);
        embeddedTestEngine = createEmbeddedTestEngine(new EngineActionsConfiguration(actionsConfiguration));
    }

    public ActionTemplateRegistry actionTemplateRegistry() {
        return actionTemplateRegistry;
    }

    public TestEngine embeddedTestEngine() {
        return embeddedTestEngine;
    }

    public Set<StepExecutionStrategy> stepExecutionStrategies() {
        return stepExecutionStrategies;
    }

    public Reporter reporter() {
        return reporter;
    }

    public ExecutionEngine executionEngine() {
        return executionEngine;
    }


    private ActionTemplateLoader createActionTemplateLoaderV2() {
        return new DefaultActionTemplateLoader<>(
            "chutney.actions",
            Action.class,
            new ActionTemplateParserV2());
    }

    private SpelFunctions createSpelFunctions() {
        SpelFunctionCallback spelFunctionCallback = new SpelFunctionCallback();
        ExtensionLoaders
            .classpathToClass("META-INF/extension/chutney.functions")
            .load().forEach(c -> ReflectionUtils.doWithMethods(c, spelFunctionCallback));

        return spelFunctionCallback.getSpelFunctions();
    }

    private Set<StepExecutionStrategy> createStepExecutionStrategies() {
        return ExtensionLoaders
            .classpathToClass("META-INF/extension/chutney.strategies")
            .load()
            .stream()
            .map(ThrowingFunction.toUnchecked(ExecutionConfiguration::<StepExecutionStrategy>instantiate))
            .map(identity(c -> LOGGER.debug("Loading strategy: " + c.getType() + " (" + c.getClass().getSimpleName() + ")")))
            .collect(Collectors.toSet());
    }

    private Reporter createReporter() {
        return new Reporter(reporterTTL);
    }

    private ExecutionEngine createExecutionEngine(ExecutorService actionExecutor, String user, String password) {
        return new DefaultExecutionEngine(
            new StepDataEvaluator(spelFunctions),
            new StepExecutionStrategies(stepExecutionStrategies),
            new DelegationService(new DefaultStepExecutor(actionTemplateRegistry), new HttpClient(user, password)),
            reporter,
            actionExecutor);
    }

    private TestEngine createEmbeddedTestEngine(ActionsConfiguration actionsConfiguration) {
        return new EmbeddedTestEngine(executionEngine, reporter, new ExecutionManager(), actionsConfiguration);
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiate(Class<?> clazz) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return (T) clazz.getDeclaredConstructor().newInstance();
    }
}
