<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Custom strategy

Create a custom strategy when the [built-in strategies](../strategies/introduction.md)
do not provide the step execution behavior you need.

# Implement

Implement `StepExecutionStrategy` in a Java class. The class must have a public
no-argument constructor so Chutney can instantiate it at startup.

This simple strategy adds information to the execution report and then delegates
the execution to Chutney's default strategy:

``` java
package my.custom.package;

import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContext;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.engine.domain.execution.strategies.DefaultStepExecutionStrategy;
import fr.enedis.chutney.engine.domain.execution.strategies.StepExecutionStrategies;
import fr.enedis.chutney.engine.domain.execution.strategies.StepExecutionStrategy;
import java.util.Map;

public class LoggedStrategy implements StepExecutionStrategy {

    public LoggedStrategy() {
    }

    @Override
    public String getType() {
        return "logged";
    }

    @Override
    public Status execute(ScenarioExecution scenarioExecution,
                          Step step,
                          ScenarioContext scenarioContext,
                          Map<String, Object> localContext,
                          StepExecutionStrategies strategies) {
        step.addInformation("Executing with the logged strategy");
        return DefaultStepExecutionStrategy.instance.execute(
            scenarioExecution,
            step,
            scenarioContext,
            localContext,
            strategies
        );
    }
}
```

`getType()` returns the identifier used by scenarios. It must be unique among
all registered strategies.

`execute()` controls the complete execution of the step. A custom strategy can
execute, repeat, skip, or delegate the step as needed. When delegating, pass the
provided `localContext` and `strategies` so nested strategies and iteration
variables continue to work.

# Package

1. Create the following file in your extension JAR:

    ``` text
    META-INF/extension/chutney.strategies
    ```

2. Add the fully qualified name of each custom strategy, one per line:

    ``` text
    my.custom.package.LoggedStrategy
    ```

3. Put the JAR containing the implementation and registration file on the
   Chutney engine classpath.

    !!! info "Custom strategy startup log"
        With debug logging enabled, Chutney logs the loaded type and class:

        ``` text
        Loading strategy: logged (LoggedStrategy)
        ```

!!! tip "Add custom strategies to an already packaged Chutney server"

    Package the implementation and `META-INF/extension/chutney.strategies` in a
    JAR, then use Spring Boot's
    [`loader.path`](https://docs.spring.io/spring-boot/specification/executable-jar/property-launcher.html){:target=_blank}
    system property to add the JAR to the server classpath.

# Use

Use the generic Kotlin DSL `Strategy` with the type returned by `getType()`:

``` kotlin
val scenario = Scenario(title = "Use a custom strategy") {
    When("Execute and add report information", Strategy(type = "logged")) {
        SuccessAction()
    }
}
```

To pass parameters, provide them through the DSL and read them from
`step.strategy().get().strategyProperties` in the implementation:

``` kotlin
Strategy(
    type = "my-strategy",
    parameters = mapOf("myParameter" to "my value")
)
```
