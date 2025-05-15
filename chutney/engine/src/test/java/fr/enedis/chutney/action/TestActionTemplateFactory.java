/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action;

import fr.enedis.chutney.action.domain.ActionInstantiationFailureException;
import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.UnresolvableActionParameterException;
import fr.enedis.chutney.action.domain.parameter.Parameter;
import fr.enedis.chutney.action.domain.parameter.ParameterResolver;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class TestActionTemplateFactory {
    private TestActionTemplateFactory() {
    }

    public static ActionTemplate buildActionTemplate(String actionType, Class<?> implementationClass) {
        return new ActionTemplate() {
            @Override
            public String identifier() {
                return actionType;
            }

            @Override
            public Class<?> implementationClass() {
                return implementationClass;
            }

            @Override
            public Set<Parameter> parameters() {
                return Collections.emptySet();
            }

            @Override
            public Action create(List<ParameterResolver> parameterResolvers) throws UnresolvableActionParameterException, ActionInstantiationFailureException {
                throw new RuntimeException(TestAction.class.getSimpleName() + "s are not instantiable");
            }
        };
    }

    public interface TestAction {
        Object execute();
    }

    public static class TestAction1 implements TestAction {
        @Override
        public Object execute() {
            return null;
        }
    }

    public static class TestAction2 implements TestAction {
        private final Map<String, Object> inputs;

        public TestAction2(Map<String, Object> inputs) {
            this.inputs = inputs;
        }

        @Override
        public Object execute() {
            return inputs;
        }
    }

    public static class TestAction3 implements TestAction {
        @Override
        public Object execute() {
            throw new IllegalStateException("test error");
        }
    }

    public static class ValidSimpleAction implements Action {

        @Override
        public ActionExecutionResult execute() {
            return ActionExecutionResult.ok();
        }
    }

    public static class ComplexAction implements Action {

        private final String someString;
        private final Pojo someObject;

        public ComplexAction(@Input("stringParam") String someString, @Input("pojoParam") Pojo someObject) {
            this.someString = someString;
            this.someObject = someObject;
        }

        @Override
        public ActionExecutionResult execute() {
            Map<String, Object> store = new HashMap<>();
            store.put("someString", someString);
            store.put("someObject", someObject);
            return ActionExecutionResult.ok(store);
        }
    }

    public static class TwoParametersAction implements Action {
        private final Map<String, Object> store = new HashMap<>();

        public TwoParametersAction(String someString, int someInt) {
            store.put("someString", someString);
            store.put("someInt", someInt);
        }

        @Override
        public ActionExecutionResult execute() {
            return ActionExecutionResult.ok(store);
        }
    }

    public static class TwoConstructorAction implements Action {

        public TwoConstructorAction(String someString) {
        }

        public TwoConstructorAction(String someString, String someString2) {
        }

        @Override
        public ActionExecutionResult execute() {
            return ActionExecutionResult.ok();
        }
    }

    public static class SuccessAction implements Action {

        @Override
        public ActionExecutionResult execute() {
            return ActionExecutionResult.ok();
        }
    }

    public static class FailAction implements Action {

        @Override
        public ActionExecutionResult execute() {
            return ActionExecutionResult.ko();
        }
    }

    public static class SleepAction implements Action {

        private final Duration duration;

        public SleepAction(@Input("duration") String duration) {
            this.duration = Duration.parse(duration);
        }

        @Override
        public ActionExecutionResult execute() {
            try {
                TimeUnit.MILLISECONDS.sleep(duration.toMilliseconds());
            } catch (InterruptedException e) {
                return ActionExecutionResult.ko();
            }
            return ActionExecutionResult.ok();
        }
    }

    public static class ContextPutAction implements Action {

        private final Map<String, Object> entries;

        public ContextPutAction(@Input("entries") Map<String, Object> entries) {
            this.entries = entries;
        }

        @Override
        public ActionExecutionResult execute() {
            return ActionExecutionResult.ok(entries);
        }
    }

    public static class ListAction implements Action {

        private final List<Map<String, Object>> list;

        public ListAction(@Input("list") List<Map<String, Object>> list) {
            this.list = list;
        }

        @Override
        public ActionExecutionResult execute() {
            return ActionExecutionResult.ok("mylist", list);
        }
    }

    public static class Pojo {
        public final String param1;
        public final String param2;

        public Pojo(@Input("param1") String param1, @Input("param2") String param2) {
            this.param1 = param1;
            this.param2 = param2;
        }
    }
}
