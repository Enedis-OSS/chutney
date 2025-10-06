<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

Following section shows how to configure the [Spring Boot server](https://docs.spring.io/spring-boot/appendix/application-properties/index.html){:target="_blank"}.

# Liquibase

--8<-- "docs/common/coming_soon.md"

# Metrics

Since Chutney relies on Spring Boot [Actuator](#actuator) and [Micrometer](https://micrometer.io/){:target="_blank"} autoconfiguration, it includes [Prometheus](https://micrometer.io/docs/registry/prometheus) by default.  
So you can find and use [default metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html#actuator.metrics.supported) : JVM, System, Datasource, Loggers, Executors and Spring MVC metrics.

Moreover, Chutney provides following metrics and corresponding Micrometer tags :

* `scenario_execution_count` counter (execution status, scenario id, scenario tags) is incremented after a scenario execution.
* `scenario_execution_timer` timer (execution status, scenario id, scenario tags) is recorded after a scenario execution.
* `scenario_in_campaign_gauge` gauge (campaign id, execution status) counts the scenario execution status after a campaign execution.
* `campaign_execution_count` counter (campaign id, campaign title, execution status) is incremented after a campaign execution.
* `campaign_execution_timer` timer (campaign id) is recorded after a campaign execution.

!!! important
    We won't thoroughly document how to collect and manage your metrics outside Chutney (even if the [demo](/getting_started/demo.md/#supervision-bonus) includes one).  
    Some hints could be :

    * Use the Actuator Prometheus endpoint to get the metrics with the appropriate format
    * Use push solution (Prometheus Pushgateway or custom)

# Authentication

!!! important
    Maven module [server](https://github.com/Enedis-OSS/chutney/tree/main/chutney/server/test/resources/blackbox){:target="_blank"} shows :

    * How to use in memory authentication and roles, see the `mem-auth` profile  
    * How to use a custom LDAP authentication (see the `ldap-auth` profile. For example purpose, it uses an embedded LDAP server)  
    * How to use a OIDC provider authentication (see the `sso-auth` profile. For example purpose, it uses a [local](https://github.com/Enedis-OSS/chutney/tree/main/chutney/server/test/resources/blackbox/sso){:target="_blank"} server)

Chutney uses Spring Security for :

* Basic authentication
* Enforce authentication and check authorization on API (ex. admin rights Spring Boot [Actuator](#actuator) endpoints)
* Configuring in memory users and roles with the Spring profile `mem-auth` if needed

!!! warning
    If you create a role name including characters 'admin' (ignoring case), all permissions will be granted to users with this role.

If you want to add another authentication mechanism, you should follow the [Spring security architecture](https://spring.io/guides/topicals/spring-security-architecture).

!!! important "Authentication requirements"
    The principal build by the authentication mechanism must be an instance of the Chutney [UserDto](https://github.com/Enedis-OSS/chutney/blob/main/chutney/server/src/main/java/fr/enedis/chutney/security/api/UserDto.java).

User roles and permissions are configured either with Web app form or by editing the file `${chutney.configuration-folder}/roles/authorization.json`.

One could use the existing [AuthenticationService](https://github.com/Enedis-OSS/chutney/blob/main/chutney/server/src/main/java/fr/enedis/chutney/security/domain/AuthenticationService.java) Chutney Spring Bean to retrieve Chutney roles by user id and grant associated authorities. <!-- TODO : I don't understand what you mean, is it useful ? provide a real use case for showing why and how it could be done -->

!!! note "How to manage permissions"
    * A user can only have one role
    * Chutney permissions are defined in the [Authorization](https://github.com/Enedis-OSS/chutney/blob/main/chutney/server-core/src/main/java/fr/enedis/chutney/server/core/domain/security/Authorization.java) class.
    * The static `grantAuthoritiesFromUserRole` method of [UserDetailsServiceHelper](https://github.com/Enedis-OSS/chutney/blob/main/chutney/server/src/main/java/fr/enedis/chutney/security/infra/UserDetailsServiceHelper.java) class could be used to have the same authentication process than `mem-auth` profile,  
    i.e. if the user has a role name containing the characters 'admin', ignoring case, user will be given all authorities available, else he will be given the authorities associated by the role retrieved by the AuthenticationService.

# Compression

Spring Boot allows to configure compression on HTTP responses payloads.

Chutney Server stores scenarios executions reports and send them over the network, so it could be useful to use this configuration.

!!! note "Server compression configuration"
    ``` yaml
    server:
        compression:
            enabled: true
            mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json # (1)
            min-response-size: 1024 # (2)
    ```
    
    1. The mime-types you want to compresse
    2. The minimum content length required for compression


# Session management

Spring Boot allows to configure session management.

!!! note "Server session configuration (with cookie)"
    ``` yaml
    server:
        servlet:
            session:
                timeout: 240m # (1)
                tracking-modes: cookie
            cookie:
                http-only: true # (2)
                secure: true # (3)
    ```

    1. The session timeout in minutes (example is 4 hours)
    2. Forbids Javascript to access the cookie
    3. Only for HTTPS requests

# Actuator

Spring Boot provides [production-ready features](https://docs.spring.io/spring-boot/reference/actuator/index.html) with the Actuator module.
Since Chutney includes this module, you can also configure it.

!!! note "Actuator configuration examples"

    === "Total deactivation"
        ``` yaml
        management:
            server:
                port: -1
            endpoints:
                enabled-by-default: false
                web:
                    exposure:
                        exclude: "*"
                jmx:
                    exposure:
                        exclude: "*"
        ```

    === "Web activation simple example"
        ``` yaml
        management:
            endpoints:
                web:
                    exposure:
                        include: "*"
            endpoint:
                health:
                    show-details: always
        ```

!!! warning
    Chutney enforces `ADMIN_ACCESS` permissions on all default Actuator endpoints.


# Specifics values

In addition of spring application [properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.server){:target="_blank"}, following table shows all properties you can set to configure Chutney.

| Name                                                    | Description                                                                                                                                                                                       | Default value                               |
|:--------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------|
| chutney.workspace                                       | Local workspace directory                                                                                                                                                                         | ~/.chutney                                  |
| chutney.configuration-folder                            | Local directory path to data and configuration files                                                                                                                                              | ${chutney.workspace}/conf                   |
| chutney.server.execution.async.publisher.ttl            | Time to live in seconds of a finished observable scenario execution                                                                                                                               | 5                                           |
| chutney.server.execution.async.publisher.debounce       | Window time in milliseconds in which a running observable scenario execution ignores new associated engine report                                                                                 | 250                                         |
| chutney.server.campaigns.executor.pool-size             | Pool size of campaigns' executor                                                                                                                                                                  | 20                                          |
| chutney.server.scheduled-campaigns.fixed-rate           | Fixed time period for scheduled campaigns execution checking                                                                                                                                      | 60000                                       |
| chutney.server.scheduled-campaigns.executor.pool-size   | Pool size of scheduled campaigns' executor                                                                                                                                                        | 20                                          |
| chutney.server.schedule-purge.cron                      | Purge launch cron planification                                                                                                                                                                   | 0 0 1 * * *                                 |
| chutney.server.schedule-purge.timeout                   | Timeout in seconds for purge (+ retries)                                                                                                                                                          | 600                                         |
| chutney.server.schedule-purge.retry                     | Number of max purge retries                                                                                                                                                                       | 2                                           |
| chutney.server.schedule-purge.max-scenario-executions   | Number of max scenario executions to keep when purging                                                                                                                                            | 10                                          |
| chutney.server.schedule-purge.max-campaign-executions   | Number of max campaign executions to keep when purging                                                                                                                                            | 10                                          |
| chutney.server.agent.name                               | Default name of local agent                                                                                                                                                                       |                                             |
| chutney.server.agent.hostname                           | Default hostname of local agent                                                                                                                                                                   |                                             |
| chutney.server.agent.network.connection-checker-timeout | Socket timeout in milliseconds for agent networking management actions                                                                                                                            | 1000                                        |
| chutney.server.editions.ttl.value                       | Time to live value of unclosed scenario's editions                                                                                                                                                | 6                                           |
| chutney.server.editions.ttl.unit                        | Time to live time [unit](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/TimeUnit.html#enum-constant-summary){:target="_blank"} of unclosed scenario's editions | HOURS                                       |
| chutney.engine.executor.pool-size                       | Pool size of scenarios' executor                                                                                                                                                                  | 20                                          |
| chutney.engine.delegation.user                          | Username of engine's delegation service HTTP client                                                                                                                                               |                                             |
| chutney.engine.delegation.password                      | Password of engine's delegation service HTTP client                                                                                                                                               |                                             |
| chutney.engine.reporter.publisher.ttl                   | Time to live in seconds of the engine's executions' reports                                                                                                                                       | 5                                           |
| chutney.actions.sql.max-logged-rows                     | Max logged rows in report for SQL action                                                                                                                                                          | 30                                          |
| chutney.actions.sql.minimum-memory-percentage-required  | Minimum percentage of JVM memory that must remain available to run the query safely                                                                                                               | 0                                           |
| chutney.auth.jwt.issuer                                 | JWT token issuer                                                                                                                                                                                  | chutney                                     |
| chutney.index-folder                                    | JWT token lifetime in minutes                                                                                                                                                                     | ${chutney.workspace}/index                  |
| chutney.server.indexes.build.time.ttl.value             | Numeric amount of time to wait for the indexer’s executor service to terminate after shutdown.                                                                                                    | 6                                           |
| chutney.server.indexes.build.time.ttl.unit              | time [unit](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/TimeUnit.html#enum-constant-summary){:target="_blank"} for that wait                                | HOURS                                       |
| chutney.security.enabled                                | Enables or disables Chutney’s security features (authentication/authorization and CORS).                                                                                                          | true                                        |
| chutney.security.cors.allowed-origins                   | List of origins (schemes, hosts, ports) allowed to access Chutney resources via CORS.                                                                                                             | https://localhost:${server.port}            |
| chutney.security.cors.allowed-methods                   | HTTP methods permitted in cross-origin requests.                                                                                                                                                  | `GET, POST, PUT, DELETE, OPTIONS, TRACE`    |
| chutney.security.cors.allowed-headers                   | HTTP request headers allowed in cross-origin requests.                                                                                                                                            | `*` (all headers)                           |
| chutney.security.cors.allow-credentials                 | Whether credentials (cookies, authorization headers, TLS certs) are allowed in CORS requests.                                                                                                     | `true`                                      |
| chutney.security.cors.max-age                           | Time in seconds a pre-flight CORS response is cached by clients.                                                                                                                                  | `3600` (1 hour)                             |
