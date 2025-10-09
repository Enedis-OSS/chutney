<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

When using Chutney to test your applications, you may need proprietary drivers, clients or use an obscure protocol not implemented by Chutney.

Chutney is packaged as a Spring Boot executable jar (fat-jar) produced by the **server** module. If you need extras (a vendor JDBC/JMS client, your own Actions/Functions, etc.), you can load external jars at runtime so you can place jars in a `lib/`(or somewhere else) folder next to the server jar and run it.

Typical use cases:

- Use Chutney with proprietary drivers or clients (e.g., JMS with WebLogic, Oracle JDBC)
- Use Chutney with an external database and authentication system (LDAP/OIDC)
- Configure logs, TLS/SSL, sessions, metrics, etc.

!!! important "Quick technical insight"

    * Chutney server is a [Spring Boot](https://docs.spring.io/spring-boot/reference/index.html){:target="_blank"} application running with [Undertow](https://undertow.io/){:target="_blank"}
    * Chutney UI is an [Angular](https://angular.io/){:target="_blank"} web application
    * Chutney is packaged as a [Spring Boot executable jar](https://docs.spring.io/spring-boot/specification/executable-jar/index.html){:target="_blank"}
    * Chutney follows Angular and Spring Boot lastest versions and corresponding dependencies

!!! tip "Configuration & Extensibility"

    For how to **configure** and **extend** chutney with external jars and configs, see **[configuration section](./configuration.md)**.
