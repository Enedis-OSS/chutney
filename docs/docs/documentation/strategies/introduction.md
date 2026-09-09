<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Step execution strategies

A strategy changes how Chutney executes a step. It can retry it, execute it for
each row of a dataset, conditionally skip it, or turn a failure into a warning.

In the Chutney Kotlin DSL, pass the strategy as the second argument of `Given`,
`When`, `Then`, `And`, or `Step`:

``` kotlin
When("The service becomes available", RetryTimeOutStrategy("5 s", "500 ms")) {
    SuccessAction()
}
```

When no strategy is specified, Chutney uses the [default strategy](default.md).

| Strategy | Kotlin DSL | Purpose |
|:---------|:-----------|:--------|
| [Default](default.md) | No argument | Execute a step normally |
| [Retry with timeout](retry.md) | `RetryTimeOutStrategy` | Retry until success or timeout |
| [Soft assert](soft.md) | `SoftAssertStrategy` | Report failures as warnings |
| [For each](foreach.md) | `ForStrategy` | Execute once for each dataset row |
| [If](if.md) | `IfStrategy` | Execute only when a condition is true |

A strategy applies to the step on which it is declared. That step may be a
technical action or a parent containing several substeps.

You can also [create and register a custom strategy](../extension/strategy.md).
