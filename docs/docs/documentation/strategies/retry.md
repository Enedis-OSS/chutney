<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Retry with timeout

`RetryTimeOutStrategy` repeats a step until it succeeds or the timeout expires.
A delay is applied between attempts.

| Parameter | Description | Example |
|:----------|:------------|:--------|
| `timeout` | Maximum total retry duration | `"5 s"` |
| `retryDelay` | Delay between two attempts | `"500 ms"` |

``` kotlin
val scenario = Scenario(title = "Wait for a service") {
    When(
        "The service becomes available",
        RetryTimeOutStrategy(timeout = "5 s", retryDelay = "500 ms")
    ) {
        HttpGetAction(
            target = "my-service",
            uri = "/health",
            validations = mapOf("Service is ready" to "status == 200".spEL())
        )
    }
}
```

The HTTP step is attempted every `500 ms`, for at most `5 s`. Execution stops
as soon as its action and validations succeed.
