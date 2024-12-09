<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

<h1>Actions</h1>

[^1]: [Here, you can see the code for all actions.](https://github.com/Enedis-OSS/chutney/tree/main/chutney/action-impl/src/main/java/com/chutneytesting/action)


Chutney provides a set of capabilities, or `Actions`, you can use in your scenarios.
They replace all the boilerplate code you would have to write and maintain for executing your scenarios.
You can see them as a set of small generic clients. [^1]

For example, instead of writing your own HTTP client for doing a POST request, you just have to use the [HttpPost](/documentation/actions/http.md/#post) action
and give it the minimum amount of information as inputs (i.e. targeted service, URI, body and headers).

All actions are structured the same way with **inputs**, **outputs**, **validations** and **teardown**.

!!! note "Extending Chutney actions"
    Actions are extensible, and you can provide your own.  
    For further details, see [how to implement your own action](/documentation/extension/action.md) and then [how to package Chutney with it](/documentation/extension/action.md/#package).

# Inputs

Inputs are the minimum information needed to run the action.  
For example, if you want to perform an HTTP GET request, you should give, at least, the targeted service and a URI.  
Obviously, you should be familiar with the technology behind each action you use, and we stick to the proper vocabulary (i.e. _body_ for HTTP, _payload_ for Kafka etc.)

In order to use some existing context variables as inputs, you need to use an [expression](#expressions) and Chutney [functions](/documentation/functions/introduction.md), so we recommend you to read about them for further details.

!!! note
    
    * Some input values are required and checked for correctness. While other values might not be required, or we provide a default value.

    * All actions performing a request on a remote service need to know the `Target`. While other action, like validating XML data, don't need a target.  
    Please, refer to actions' configuration for further details.

    * All actions must have a [Logger](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-spi/src/main/java/com/chutneytesting/action/spi/injectable/Logger.java){:target=_blank} class as input. At runtime a [DelagateLogger](https://github.com/Enedis-OSS/chutney/blob/main/chutney/engine/src/main/java/com/chutneytesting/engine/domain/execution/engine/parameterResolver/DelegateLogger.java){:target=_blank} is automatically injected by the execution engine.  
     This logger contains action's logs which be present in the execution report.

??? info "Special inputs types"
    
    * **Duration Type** <a name="duration-type"></a>
    
        Sometimes, Action input is a String of type Duration.  
        Expected format is `<positive number> <unit>`.  
        `unit` values are :
        
        * "nanos", "ns"
        * "micros", "µs"
        * "millis", "ms"
        * "seconds", "s", "sec"
        * "minutes", "m", "min"
        * "hours", "h", "hour", "hours", "hour(s)"
        * "days", "d", "day", "days", "day(s)"
        
        **Examples** : "5 min", "300 sec", "1 day"

# Outputs

Outputs contain the data collected after performing an action, and only if it succeeded.
These data are set in the execution context and can be accessed and used later in another action.

Each action provide a set of default outputs. But they are generic and may contain much more information than what you actually need.  
In order to process them, you need to use an [expression](#expressions) and Chutney [functions](/documentation/functions/introduction.md), so we recommend you to read about them for further details.

!!! note
    The execution context holds outputs in a key/value map, where the key is a string and the value is typed.

!!! warning
    Since the execution context is a map, default outputs are overridden if you run the same action more than once in the scenario or if outputs have the same name (key).

!!! tip
    We strongly recommend you to define your own outputs for setting relevant data in the execution context.

=== "Kotlin"

    ``` kotlin
    HttpGetAction(
        target = "ghibli_movie_service",
        uri = "/all?offset=0&limit=3",
        outputs = mapOf(
            "bestMovies" to "jsonPath(#body, '$.movies[?(@.rating > 85)].title')".spEL()
        )
    )
    ```

=== "JSON"

    ``` json
    {
        "type": "http-get",
        "target": "ghibli_movie_service",
        "inputs": {
            "uri": "/all?offset=0&limit=3"
        },
        "outputs": {
            "bestMovies": "${#jsonPath(#body, '$.movies[?(@.rating > 85)].title')}"
        }
    }
    ```

After executing this action, the execution context will contain the following outputs :

| Key        | Type                                 |
|:-----------|:-------------------------------------|
| body       | String                               |
| status     | Integer                              |
| headers    | org.springframework.http.HttpHeaders |
| bestMovies | List<String>                         |

Your relevant data can be accessed from another SpEL using `#bestMovies` and since it is a List you can call methods on it, like so : `${#bestMovies.get(0)}`  
`#body`, `#status` and `#headers` are also available but are very likely to be overridden by a following step while you have full control over the use of the `#bestMovies` key.

# Validations

Validations are a list of checks you want to perform in order to validate a step.
By default, a step will _fail_ when an error occurs, but we cannot verify the semantic of the result.  
Asserting a step depends on your feature and requirements.

For example, if an HTTP GET request returns a status code 500, the step is _technically_ complete and succeed.  
But, you may want to fail the step if the status is different from 200.

Each validation has a name and evaluates to a boolean, using [expressions](#expressions) and [functions](/documentation/functions/introduction.md). 

=== "Kotlin"

    ``` kotlin
    HttpGetAction(
        target = "ghibli_movie_service",
        uri = "/all?offset=0&limit=3",
        outputs = mapOf(
            "bestMovies" to "jsonPath(#body, '$.movies[?(@.rating > 85)].title')".spEL()
        ),
        validations = mapOf(
            "request_succeed" to "status == 200".spEL(),
            "found_2_movies" to "bestMovies.size() == 2".spEL()
        )
    )
    ```

=== "JSON"

    ``` json
    {
        "type": "http-get",
        "target": "ghibli_movie_service",
        "inputs": {
            "uri": "/all?offset=0&limit=3"
        },
        "outputs": {
            "bestMovies": "${#jsonPath(#body, '$.movies[?(@.rating > 85)].title')}"
        },
        "validations": {
            "request_succeed": "${#status == 200}",
            "found_2_movies": "${#bestMovies.size() == 2}"
        }
    }
    ```

# Teardown

Sometimes you may need to clean data or come back to a stable state after executing a scenario.  
Chutney provides a way to do it by _registering_ a `final action`.

!!! note
    Some actions will, by default, register a final action.  
    Most often, it is for closing resources. For example, when starting a mock SSH server, we automatically register an action to stop it at the end of the scenario.

    But we cannot provide more than that, since a teardown depends on _your_ specification and needs.

If you need to add your own final action to your scenario, it is not different from a regular action since it **is** just an action by itself !

!!! tip "Register your final action first !"
    Since a scenario execution stops at the first failure, if your final action is in a step _after_ the failure, it will never be registered nor run.  
    So you must register them before.

!!! tip "Wrap your final action with its corresponding step !"
    Since you register your final actions before anything, you still don't want to run them all when it does not make sense.  
    To avoid that, the best practice is to wrap it in a step with the corresponding action it cleans.

**Example**

``` kotlin
Step("Insert data in a table") { // (1)
    Step("Final action : delete data at the end") { // (2)
        FinalAction(
            name = "Delete data",
            type = "sql",
            target = "my_database",
            inputs = mapOf(
                "statements" to listOf("DELETE FROM MY_TABLE WHERE id=1")
            )
        )
    }
    Step("Insert data in MY_TABLE") { // (3)
        SqlAction(
            target = "my_database",
            statements = listOf(
                "insert into MY_TABLE (ID, NAME) values(1, 'my_name')"
            )
        )
    }
}
```

1. This is a wrapper step
2. We declare our final action **first** !
3. We declare our real action after

# Expressions

Chutney expressions come from [Spring EL](https://docs.spring.io/spring-framework/reference/core/expressions.html){:target="_blank"}.  
The Chutney execution context is the evaluation context of **inputs**, **outputs** and **validations** expressions.
