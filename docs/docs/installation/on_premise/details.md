<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Database

[Liquibase](https://www.liquibase.org/){:target="_blank"} is used to manage Chutney RDBMS schema.  
You can find corresponding changelog [here](https://github.com/Enedis-OSS/chutney/blob/main/chutney/server/src/main/resources/changelog/db.changelog-master.xml){:target="_blank"}.

!!! note
    * Chutney is tested with SQLite, H2 and PostgreSQL databases. 
    * Chutney configure those database throught spring profiles. `db-h2` for H2, `db-sqlite `or `db-sqlite-rw`(different datasources for read and write) for SQLite and `db-pg` for PostgreSQL
    * You can find complete examples in maven module [chutney/server](https://github.com/Enedis-OSS/chutney/tree/main/chutney/server){:target="_blank"}, for all three database types.

To configure your datasource, use the property `spring.datasource`

=== "SQLite"
    ``` yaml
    spring:
        datasource:
            url: jdbc:sqlite:.chutney/data/chutney.db
    ```

=== "H2 (memory)"
    ``` yaml
    spring:
        datasource:
            url: jdbc:h2:mem:dbName
    ```

=== "PostgreSQL (SSL two way)"
    ``` yaml
    spring:
        datasource:
            url: jdbc:postgresql://host:port/dbName?ssl=true&sslmode=verify-ca&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory&currentSchema=mySchema
            username: user
    ```

# Logs

Chutney uses [SLF4J](https://www.slf4j.org/) with [Logback](https://logback.qos.ch/) as the runtime implementation.  
A default [logback.xml](https://github.com/Enedis-OSS/chutney/blob/main/chutney/server/src/main/resources/logback.xml){:target=_blank} is packaged in the server jar and logs to the console at level `WARN`.

!!! warning
Do **not** add logging bridges yourself — they are already included.  
Avoid:
* `jcl-over-slf4j`
* `log4j-over-slf4j` / `slf4j-reload4j`
* `jul-to-slf4j`

For complete details, see the Spring boot [logging](https://docs.spring.io/spring-boot/reference/features/logging.html){:target=_blank} documentation.  
Below are just a few common examples.
## Override the log configuration

You can replace or adjust the default logging without repackaging.

### Use a custom Logback file

```shell
# JVM system property
java -Dlogging.config=file:./my-logback.xml -jar server-<version>-boot.jar

# or environment variable
LOGGING_CONFIG=file:./my-logback.xml java -jar server-<version>-boot.jar
```

### Quick tweaks via `application.yml`

For simple level changes:

```yaml
logging:
  level:
    root: INFO
    com.company.yourpackage: DEBUG
```

Use a full Logback file if you need to change appenders or patterns.

# Server (TLS/SSL)

Chutney server enforces the use of secure calls on any incoming requests.

!!! note "Server HTTPS configuration"
    ``` yaml
    server:
        port: 443
        ssl:
            keystore: # keystore path
            key-store-password: # keystore password
            key-password: # key password
            trust-store: # truststore path
            trust-store-password: # truststore password
    ```

Chutney Server provides `undertow-https-redirect` Spring profile to redirect unsecured request to the right secured port.

??? note "Using `undertow-https-redirect` Spring profile"

    * Activate the profile

    ``` yaml
    spring:
        profiles:
            active:
              - undertow-https-redirect
    ```

    * Configure the HTTP listener

    ``` yaml
    server:
        http:
            port: 80 # (1)
            interface: 0.0.0.0 # (2)
    ```

    1. HTTP port to use
    2. Interface to bind to

