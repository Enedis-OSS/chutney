<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Configuration & Extensibility

Chutney is packaged as a Spring Boot executable jar (fat-jar) built from the **server** module.  
It is both **configurable** and **extensible**:

## Configurable

Override the default `application.yml` using an [**external configuration**](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files){:target=_blank} file or directory:

```bash
# JVM system property
java -Dspring.config.location=./my-config/application.yml -jar server-<version>.jar
```

or with an **environment variable**:

```bash
SPRING_CONFIG_LOCATION=./my-config/application.yml
java -jar server-<version>.jar
```

Use this to customize database, authentication, roles/permissions, [logging](./details.md/#logs), TLS, and more.

### Overriding only some properties

You don’t have to replace the full packaged `application.yml`.  
Spring Boot merges configuration files in the following order of precedence:

- Values in your external `application.yml` **override only the keys you redefine**.
- Any unspecified properties still fall back to the defaults packaged inside the server jar.

For example, to change only the server port and logging level:

```yaml
# my-config/application.yml
server:
  port: 8081

logging:
  level:
    root: INFO
```

Everything else (database, security, etc.) continues to use the defaults provided in the jar.

### Override properties inline when starting the jar

Instead of creating an external config file, you can also override individual properties directly:

```bash
# Using JVM system properties
java -Dserver.port=8081 -Dlogging.level.root=INFO -jar server-<version>.jar
```

```bash
# Using environment variables
SERVER_PORT=8081 LOGGING_LEVEL_ROOT=INFO java -jar server-<version>.jar
```

This is useful for quick overrides or CI/CD pipelines where you inject only a few values.

!!! warning "Handling secrets"
How to handle secrets in configuration files varies a lot and depends on your CI/CD so this documentation does not cover this topic.  
One example, if you use [Ansible](https://docs.ansible.com/ansible/latest/index.html), you can package a subset of configuration files, select and filter them during deployment, so they will be included in the runtime classpath of the application.   
In the server default packaging we are using [jasypt](https://github.com/Enedis-OSS/chutney/tree/main/chutney/server/src/main/resources/security/jasypt/README.md){:target=_blank} to encode sensitive data.


## Extensible

Because Chutney is a Spring Boot app (ZIP layout + PropertiesLauncher), you can add **proprietary drivers** or custom **extensions**([Actions](/documentation/extension/action.md) or [Functions](/documentation/extension/function.md)) at runtime by putting jars on the classpath:

```bash
# JVM system property
java -Dloader.path=lib -jar server-<version>.jar
```

or with an **environment variable**:

```bash
LOADER_PATH=lib
java -jar server-<version>.jar
```

Place your jars under `./lib` and they will be added to the classpath automatically.

