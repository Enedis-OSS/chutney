<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

<h1>Custom function</h1>

When you need a custom function which is not provided by Chutney, you can implement it and load it to your chutney server.

# Implement

 * Create a new java class.
 * Declare a static method and implement it.
 * Annotate it with `@SpelFunction`.

!!! warning
    Method overloading does not work with SpEL.

=== "Custom function"

``` java
    package my.custom.package;

    import fr.enedis.chutney.action.spi.SpelFunction;
    import org.apache.commons.lang3.StringUtils;

    public class MyCustomFunctions {

        @SpelFunction
        public static int stringSum(String a, String b) {
            int right = StringUtils.isNoneBlank(a) ? Integer.valueOf(a) : 0;
            int left = StringUtils.isNoneBlank(b) ? Integer.valueOf(b) : 0;
            return left + right;
        }
}
```

# Package

 1. Declare your custom actions full class names inside the file `META-INF/extension/chutney.functions`.
   ```
   my.custom.package.MyCustomFunctions
   ```

 2. Add your custom functions' code and meta file in the classpath of a Chutney engine.

    !!! info "Custom function starting server debug log"
        Check your server log, you will see something like  
        ```
        [main] DEBUG c.c.e.d.e.evaluation.SpelFunctions - Loading function: stringSum (MyCustomFunctions)
        ```

!!! tip "Add custom functions to an already packaged Chutney server"

    1. Package a JAR archive with your custom functions and associated meta file.
    2. Use the [`loader.path`](https://docs.spring.io/spring-boot/specification/executable-jar/property-launcher.html){:target=_blank} Java system properties to add your archive to classpath.


# Use

Call your custom function from your Kotlin scenario.

``` kotlin
    import fr.enedis.chutney.kotlin.dsl.AssertAction
    import fr.enedis.chutney.kotlin.dsl.Scenario

    val my_scenario = Scenario(title = "my scenario") {
        When("I test my string sum function") {
            AssertAction(
                asserts = listOf(
                    "stringSum('1', '2') == 3".spEL(),
                    "stringSum('1', null) == 1".spEL(),
                    "stringSum(null, '2') == 2".spEL(),
                    "stringSum(null, null) == 0".spEL(),
                ),
            )
        }
    }
```
