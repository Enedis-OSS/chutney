<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# For each

`ForStrategy` executes a step once for each row of a dataset. The values of the
current row are available in the execution context during that iteration.

| Parameter | Description | Default |
|:----------|:------------|:--------|
| `dataset` | Expression resolving to a list of maps | `"dataset".spEL` |
| `index` | Name of the zero-based iteration index | `"i"` |

``` kotlin
val scenario = Scenario(title = "Greet every user") {
    Given("Some users") {
        ContextPutAction(
            entries = mapOf(
                "users" to listOf(
                    mapOf("name" to "Alice"),
                    mapOf("name" to "Bob")
                )
            )
        )
    }
    When("Greet user <i>: \${#name}", ForStrategy(dataset = "users".spEL())) {
        DebugAction(filters = listOf("name"))
    }
}
```

The `When` step runs twice. For each iteration, `${#name}` resolves to the
current user's name and `<i>` is replaced by `0`, then `1`.

The dataset must contain at least one row.
