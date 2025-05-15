<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

??? info "Browse implementations"

    - [Context Put](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/ContextPutAction.java){:target="_blank}
    - [Debug](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/DebugAction.java){:target="_blank"}
    - [Sleep](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/SleepAction.java){:target="_blank"}
    - [Final](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/FinalAction.java){:target="_blank"}
    - [Fail](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/FailAction.java){:target="_blank"}
    - [Success](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/SuccessAction.java){:target="_blank"}


A context action is a special case of action in Chutney, providing technical support :

* It does not make use of external services (see [target](/documentation/environment.md/#target)).
* It does not define outputs.
* It should not have [validations](/documentation/actions/introduction.md/#validations) included.

# Context Put

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/ContextPutAction.java){:target="_blank"}"

Take a list of named values and put them into the execution context (i.e. create a variable for scenario [expressions](/documentation/actions/introduction.md/#expressions)).
=== "Inputs"

    | Required | Name      | Type                     | Default |
    |:--------:|:----------|:-------------------------|:--------|
    |    *     | `entries` | Map of <String, Object\> |         |

=== "Outputs"
    No outputs

### Example
=== "Kotlin"
    ``` kotlin
    ContextPutAction(
        entries = mapOf(
            "startDate" to "now().toInstant()".spEL(),
            "isoFormatter" to "isoDateFormatter('instant')".spEL(),
            "uuid" to "generate().uuid()".spEL()
        )
    )
    ```

# Debug

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/DebugAction.java){:target="_blank"}"

Log the execution context variables.

=== "Inputs"

    | Required | Name      | Type          | Default | Description                                    |
    |:--------:|:----------|:--------------|:--------|:-----------------------------------------------|
    |    *     | `filters` | List<String\> |         | List of strings that logged keys must contains |

=== "Outputs"
    No outputs

### Example
=== "Kotlin"
    ``` kotlin
    DebugAction(
        filters = listOf("date")
    )
    ```

# Sleep

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/SleepAction.java){:target="_blank"}"

Wait for a given time.

=== "Inputs"

    | Required | Name       | Type                                                                | Default | Description          |
    |:--------:|:-----------|:--------------------------------------------------------------------|:--------|:---------------------|
    |    *     | `duration` | [Duration](/documentation/actions/introduction.md/#duration-type) (String) |         | The time to wait for |

=== "Outputs"
    No outputs

### Example
=== "Kotlin"
    ``` kotlin
    SleepAction(
        duration =  "5 sec"
    )
    ```

# Final

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/FinalAction.java){:target="_blank"}"

Define a [teardown](/documentation/actions/introduction.md/#teardown) action, which will be executed at the end of scenario execution.

=== "Inputs"

    | Required | Name                  | Type                 | Default | Description                           |
    |:--------:|:----------------------|:---------------------|:--------|:--------------------------------------|
    |    *     | `type`                | String               |         | The action type                       |
    |    *     | `name`                | String               |         | The name of the teardown action step  |
    |          | `inputs`              | Map<String, Object\> |         | The inputs to use                     |
    |          | `validations`         | Map<String, Object\> |         | The validations to execute            |
    |          | `strategy-type`       | String               |         | The type of strategy to use           |
    |          | `strategy-properties` | Map<String, Object\> |         | The properties of the strategy to use |

=== "Outputs"
    No outputs

### Example
=== "Kotlin"
    ``` kotlin
    FinalAction(
        name = "Assert time passes...",
        type = "sleep",
        inputs = mapOf(
            "duration" to "500 ms"
        ),
        strategyType = "retry-with-timeout",
        strategyProperties = mapOf(
            "timeOut" to "3 s",
            "retryDelay" to "1 s"
        ),
        validations = mapOf(
            "date is past" to "now().isAfter(#dateToPass)".spEL()
        )
    )
    ```

# Fail

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/FailAction.java){:target="_blank"}"

Just fail.

=== "Inputs"
    No inputs

=== "Outputs"
    No outputs


### Example
=== "Kotlin"
    ``` kotlin
    FailAction()
    ```

# Success

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/context/SuccessAction.java){:target="_blank"}"

Just pass.

=== "Inputs"
    No inputs

=== "Outputs"
    No outputs


### Example
=== "Kotlin"
    ``` kotlin
    SuccessAction()
    ```
