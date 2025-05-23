<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/function/NullableFunction.java){:target="_blank"}"

This function helps you handle values which may be null.

!!! note "Object nullable(Object input)"

    See [Optional.ofNullable()](https://devdocs.io/openjdk~21/java.base/java/util/optional#ofNullable(T)){:target="_blank"} for further details

    **Returns** :
    
    * The typed value or the String "null" in case the value was null.

    **Examples** :

    SpEL without : `${T(java.util.Optional).ofNullable(#mayBeNull).orElse("null")}`

    SpEL with    : `${#nullable(#mayBeNull)}`
