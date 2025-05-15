<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/micrometer/MicrometerFunctions.java){:target="_blank"}"

Following functions help you work with Micrometer application monitoring facade.

!!! note "MeterRegistry micrometerRegistry(String registryClassName)"

    Retrieve an existing Micrometer registry by its class name.

    **Parameters** :

    * `registryClassName` : The searched Micrometer registry class name

    **Returns** : The searched registry or global registry if not found

    **Examples** :

    SpEL : `${#micrometerRegistry('CustomMeterRegistry')}`
