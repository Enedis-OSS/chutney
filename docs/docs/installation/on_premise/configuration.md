<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->


Chutney is packaged as a Spring Boot executable jar (fat-jar) built from the **server** module.  
That means you configure and extend it the same way you would any other Spring Boot application.

See the official Spring Boot documentation on [externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html){:target=_blank} for complete details.   
Use this to customize database, authentication, roles/permissions, [logging](./details.md/#logs), TLS, and more.   
Below are just a few common examples.

---

## Configurable

You can override the packaged `application.yml` with an external file or directory:

```shell
# JVM system property
java -Dspring.config.location=./my-config/application.yml -jar chutney-server-<version>.jar

# or environment variable
SPRING_CONFIG_LOCATION=./my-config/application.yml java -jar chutney-server-<version>.jar
```

### Overriding only some properties

You don’t need to copy the full config: Spring Boot merges files, so you can redefine just a subset:

```yaml
# my-config/application.yml
server:
  port: 8081

logging:
  level:
    root: INFO
```

### Override inline at startup

For quick tweaks or CI/CD pipelines you can pass properties directly:

```shell
# JVM system properties
java -Dserver.port=8081 -Dlogging.level.root=INFO -jar chutney-server-<version>.jar

# Environment variables
SERVER_PORT=8081 LOGGING_LEVEL_ROOT=INFO java -jar chutney-server-<version>.jar
```

!!! warning "Handling secrets"
    Handling secrets depends on your CI/CD environment(Vault, Jasypt, etc).

---

## Extensible

Because Chutney uses Spring Boot’s [PropertiesLauncher](https://docs.spring.io/spring-boot/specification/executable-jar/property-launcher.html){:target="_blank"}, you can add proprietary drivers or custom extensions([Actions](/documentation/extension/action.md) or [Functions](/documentation/extension/function.md)) at runtime by dropping jars into a directory and pointing to it:

```shell
# JVM system property
java -Dloader.path=lib -jar chutney-server-<version>.jar

# or environment variable
LOADER_PATH=lib java -jar chutney-server-<version>.jar
```

Everything under `./lib` is added to the classpath automatically.
