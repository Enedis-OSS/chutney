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
    * You can find complete examples in maven module [chutney/server](https://github.com/Enedis-OSS/chutney/tree/main/chutney/server/src/main/resources){:target="_blank"}, for all three database types.

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

Chutney depends on [SLF4J](https://www.slf4j.org/) API logging library.

At runtime, the Chutney server uses the [Logback](https://logback.qos.ch/) SLF4J implementation and bridges legacy APIs (JCL, LOG4J and JUL).

!!! warning
    
    Since the server bridges all legacy APIs, you must be careful to not include any of the following libraries:

    * jcl-over-slf4j
    * log4j-over-slf4j and slf4j-reload4j
    * jul-to-slf4j

    Read [Bridging legacy APIs](https://logback.qos.ch/manual/configuration.html) for further details.

A **default Logback configuration** is packaged ([here](https://github.com/Enedis-OSS/chutney/blob/main/chutney/server/src/main/resources/logback.xml){:target=_blank}) inside the server jar (classpath root).  
By default it logs to the console at level `WARN`.

## **External** Log config

You can override the packaged logging config. Choose one of the following options for example. See details [here](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.custom-log-configuration){:target=_blank}

### Option A — Point to a file with `logging.config` (recommended)

Use a JVM system property or an environment variable:

```bash
# JVM system property
java -Dlogging.config=file:./my-logback.xml -jar server-<version>.jar

# Environment variable
LOGGING_CONFIG=file:./my-logback.xml \
java -jar server-<version>.jar
```

Supported file names are typically `logback.xml` or `logback-spring.xml`.  
Using `file:` ensures the file on disk is picked up even if a default config is packaged.

### Option B — Put a config file on the classpath

Spring Boot also looks for Logback config on the classpath (e.g., `logback-spring.xml` or `logback.xml`).  
If you want to keep the file next to the jar, place it under `./config` which is added to the classpath automatically:

```
.
├─ chutney-server-<version>.jar
└─ config/
   └─ logback-spring.xml
```

Then start normally:

```bash
java -jar chutney-server-<version>.jar
```

> Prefer `logback-spring.xml` when you want to use Spring profile conditions (`<springProfile>`).

### Option C — Basic tweaks via `application.yml`

For simple level changes you can stay in Spring config:

```yaml
logging:
  level:
    root: INFO
    com.company.yourpackage: DEBUG
```

This won’t replace appenders/layouts; for full control use **Option A** or **B**.

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

