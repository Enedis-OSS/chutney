<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/function/SoapFunction.java){:target="_blank"}"

Following functions help you work with SOAP.

!!! note "String soapInsertWSUsernameToken(String user, String password, String envelope)"

    Creates WS username token, build an associated security header and inserts it as child into the given SOAP Envelope.

    **Parameters** :

    * `user` : The username to use
    * `password` : The password to use
    * `envelope` : The soap envelope to update

    **Returns** : The soap envelope with the security header

    **Examples** :

    SpEL : `${#soapInsertWSUsernameToken('username', 'password', #soapEnvelope)}`
