<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

A Chutney scenario is a succession of named steps with a simple goal, asserting an expected behavior of a given system.

A Chutney Step could be of two flavors :

* A purely functional step grouping others `child` steps, allowing to abstract business requirements and structure functional clarity.
* A technical step describing a concrete [action](/documentation/actions/introduction.md).

--8<-- "docs/getting_started/write.md"
