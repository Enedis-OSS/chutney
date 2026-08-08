<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Default strategy

The default strategy executes a step once. For a parent step, it executes its
substeps in declaration order and stops when one fails.

It has no DSL class or parameters: omit the `strategy` argument.

``` kotlin
val scenario = Scenario(title = "Default strategy") {
    When("Process the order") {
        Step("Validate the order") {
            SuccessAction()
        }
        Step("Save the order") {
            SuccessAction()
        }
    }
}
```

Here, `Validate the order` runs before `Save the order`.
