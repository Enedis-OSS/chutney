<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

<h1>Functions</h1>

[^1]: [Here, you can see the code for all functions.](https://github.com/Enedis-OSS/chutney/tree/main/chutney/action-impl/src/main/java/com/chutneytesting/action/function)


Chutney provides a set of procedures, or `Functions`, you can use in your scenarios' expressions.  
They allow to read scenarios and their executions reports with a better functional point of view.  
You can see them as a set of aliases, allowing to simplify the processing of scenario execution context variables. [^1]

Those are based on [spEL Functions](https://docs.spring.io/spring-framework/reference/core/expressions/language-ref/functions.html).

!!! note "Extending Chutney functions"
    Actions are extensible, and you can provide your own.  
    For further details, see [how to implement your own function](/documentation/extension/function.md) and then [how to package Chutney with it](/documentation/extension/function.md/#package).

# Example

Let's say you made an HTTP request which get some json movies' list and put it in context variable `body`.  
You now want to get the list of movie titles rated above 85/100 from it, in order to make an assertion.

```json title="Existing 'body' variable in context"
{
    "movies": [
        {
            "title": "Castle in the Sky",
            "director": "Hayao Miyazaki",
            "rating": 78
        },
        {
            "title": "Grave of the Fireflies",
            "director": "Isao Takahata",
            "rating": 94
        },
        {
            "title": "My Neighbor Totoro",
            "director": "Hayao Miyazaki",
            "rating": 86
        }
    ]
}
```

The best way to filter and extract the relevant data from a JSON document is to use a JSONPath expression.  
Here is the one for our example : `$.movies[?(@.rating > 85)].title`

In order to process it, you would need to write code using a JSONPath library and then tell Chutney to run your custom code.  
Here is a raw expression you could write :  
`${T(com.jayway.jsonpath.JsonPath).parse(#body).read("$.movies[?(@.rating > 85)].title")}`  
In this case you can use the `jsonPath` function, provided by Chutney, and the resulting SpEL would become :  
`${#jsonPath(#body, '$.movies[?(@.rating > 85)].title')}`
