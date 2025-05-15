<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/function/MathFunctions.java){:target="_blank"}"

Following functions help you write and shorten SpEL when you need to perform math operations.

## abs

!!! note "Number abs(Number a)"

    Returns the absolute value of a given number.  
    ex. Number -2 to 2.

    See [Math.abs()](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#abs-int-){:target="_blank"} for further details

    **Parameters** :

    * `Number a` : The integer you want to get an absolute value
        * ex. -2

    **Returns** :

    * An absolute value of a given number.

    **Examples** :

    SpEL without : `${T(java.lang.Math).abs(-2)}`

    SpEL with    : `${#abs(-2)}`

## min

!!! note "Number min(Number a, Number b)"

    Returns the smaller value of a and b.  
    ex. Min of 4L and 9L is 4L.


    See [Math.min()](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#min-int-int-){:target="_blank"} for further details

    **Parameters** :

    * `Number a` : A number
        * ex. 4L
    * `Number b` : Another number
        * ex. 9L

    **Returns** :

    * The smaller value of a and b.

    **Examples** :

    SpEL without : `${T(java.lang.Math).min(4L, 9L)}`

    SpEL with    : `${#min(4L, 9L)}`

## max

!!! note "Number max(Number a, Number b)"

    Returns the greater value of a and b.  
    ex. Min of 4f and 9f is 9f.

    See [Math.max()](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#max-int-int-){:target="_blank"} for further details

    **Parameters** :

    * `Number a` : A number
        * ex. 4f
    * `Number b` : Another number
        * ex. 9f

    **Returns** :

    * The greater value of a and b.

    **Examples** :

    SpEL without : `${T(java.lang.Math).max(4f, 9f)}`

    SpEL with    : `${#max(4f, 9f)}`
