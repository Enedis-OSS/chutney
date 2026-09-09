<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# If

`IfStrategy` executes a step only when its condition evaluates to `true`. When
the condition is false, the step and all its substeps are skipped and reported
as successful.

| Parameter | Description |
|:----------|:------------|
| `condition` | A boolean or an expression returning a boolean |

``` kotlin
val scenario = Scenario(title = "Conditional notification") {
    Given("Notification preference") {
        ContextPutAction(entries = mapOf("notificationsEnabled" to true))
    }
    When(
        "Send the notification",
        IfStrategy(condition = "notificationsEnabled".spEL())
    ) {
        SuccessAction()
    }
}
```

The `When` step runs only when `notificationsEnabled` is `true`.
