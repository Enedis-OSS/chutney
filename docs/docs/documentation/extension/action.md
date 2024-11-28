<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

<h1>Custom action</h1>

# Implement

=== "Custom action 1"

    ``` java
    package my.custom.package

    import com.chutneytesting.action.spi.Action;
    import com.chutneytesting.action.spi.injectable.Input;
    import com.chutneytesting.action.spi.injectable.Logger;

    public class CustomAction implements Action {

        private final Logger logger;
        private final String parameter;

        public CustomAction(Logger logger,
                            @Input("parameter") String parameter) {
            this.logger = logger;
            this.parameter = parameter;
        }

        @Override
        public ActionExecutionResult execute() {
            logger.info("My custom action");
            return ActionExecutionResult.ok();
        }

        @Override
        public List<String> validateInputs() {
            return Action.super.validateInputs();
        }
    }
    ```

=== "Custom action 2"

    ``` java
    package my.custom.package

    import com.chutneytesting.action.spi.Action;
    import com.chutneytesting.action.spi.injectable.Input;
    import com.chutneytesting.action.spi.injectable.Logger;
    import com.chutneytesting.action.spi.injectable.Target

    public class CustomAction2 implements Action {

        private final Target target;
        private final Logger logger;
        private final String parameter;

        public CustomAction(Target target,
                            Logger logger,
                            @Input("parameter") String parameter) {
            this.target = target;
            this.logger = logger;
            this.parameter = parameter;
        }

        @Override
        public ActionExecutionResult execute() {
            logger.info("My custom action");
            if (parameter.equals("parameter_value")) {
                String outputObject = "output value";
                return ActionExecutionResult.ok(Map.of("outputKey", outputObject));
            } else {
                logger.error("parameter is not valid !");
                return ActionExecutionResult.ko();
            }
        }

        @Override
        public List<String> validateInputs() {
            return Action.super.validateInputs();
        }
    }
    ```

# Package

 1. Create a `chutney.actions` in resources/META-INF/extension.

 2. Declare your custom actions full class names inside it
    ```
    my.custom.package.CustomAction1
    my.custom.package.CustomAction2
    ```

 3. Restart Chutney server and all declared actions are now loaded.

    !!! info "Custom action starting server debug log"
        Check your server log, you will see something like  
        ```
        [main] DEBUG c.c.a.d.DefaultActionTemplateRegistry - Action registered: custom-action-1 (my.custom.package.CustomAction1)
        [main] DEBUG c.c.a.d.DefaultActionTemplateRegistry - Action registered: custom-action-2 (my.custom.package.CustomAction2)
        ```

# Use

=== "Kotlin 1"
    ``` kotlin
    CustomAction(
        parameter = "parameter_value"
    )
    ```
=== "Kotlin 2"
    ``` kotlin
    CustomAction2(
        target = "CUSTOM_TARGET",
        parameter = "some value",
    )
    ```
