<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Soft assert

`SoftAssertStrategy` changes a failure into a warning. On a parent step, all
substeps are executed even if one of them fails. This is useful for collecting
several assertion results in a single execution report.

It has no parameters.

``` kotlin
val scenario = Scenario(title = "Check several rules") {
    Then("All rules are checked", SoftAssertStrategy()) {
        Step("A failing rule") {
            FailAction()
        }
        Step("This rule is checked too") {
            SuccessAction()
        }
    }
}
```

Both substeps are executed. The parent step is reported as a warning rather
than a failure.
