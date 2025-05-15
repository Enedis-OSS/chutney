<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

??? info "Browse implementations"

    === "JMS"

        - [Sender](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jms/JmsSenderAction.java){:target="_blank"}
        - [Listener](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jms/JmsListenerAction.java){:target="_blank"}
        - [Clean queue](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jms/JmsCleanQueueAction.java){:target="_blank"}

    === "Jakarta"

        - [Sender](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jakarta/JakartaSenderAction.java){:target="_blank"}
        - [Listener](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jakarta/JakartaListenerAction.java){:target="_blank"}
        - [Clean queue](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jakarta/JakartaCleanQueueAction.java){:target="_blank"}

!!! note "Define a jms or jakarta target"

    * Default `connectionFactoryName` is `ConnectionFactory`
    * To configure ssl, by default we add these properties in InitialContext : 
        * `connection.ConnectionFactory.keyStore` with `keyStore` property
        * `connection.ConnectionFactory.keyStorePassword` with `keyStorePassword` property
        * `connection.ConnectionFactory.keyStoreKeyPassword` with `keyPassword` property
        * `connection.ConnectionFactory.trustStore` with `trustStore` property
        * `connection.ConnectionFactory.trustStorePassword` with `trustStorePassword` property
    * All configuration beginning with java.naming.* are added to the context 
    * Other configuration:  
    In order to provide more configuration, you should prefix all other target properties with `jndi.`.  
    By example, if you want to add `com.specific.vendor.properties` key, the key should be `jndi.com.specific.vendor.properties`

```json title="Jms/Jakarta target example"
{
    "name": "JMS_TARGET",
    "url": "ssl://my.jms.server:61616",
    "properties": {
        "connectionFactoryName": "MyConnectionFactory",
        "java.naming.factory.initial": "org.apache.activemq.jndi.ActiveMQInitialContextFactory",
        
        "username": "myUsername", // (1)
        "password": "myPassword", // (2)
        "trustStore": "/home/APP/security/mytruststore.jks",
        "trustStorePassword": "myTrustStorePassword",
        "keyStore": "/home/APP/security/mykeyStore.jks",
        "keyStorePassword": "mykeyStorePassword",
        "keyPassword": "myKeyStoreKeyPassword"
    }
}
```

1. Valid properties are `username` or `user`. Set this for basic authentication
2. Valid properties are `userPassword` or `password`. Set this for basic authentication

# Sender

!!! info "Browse [JMS](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jms/JmsSenderAction.java){:target="_blank"} or [Jakarta](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jakarta/JakartaSenderAction.java){:target="_blank"} implementation"

=== "Inputs"

    | Required | Name          | Type                              |  Default   |
    |:--------:|:--------------|:----------------------------------|:----------:|
    |    *     | `target`      | String                            |            |
    |    *     | `destination` | String                            |            |
    |    *     | `body`        | String                            |            |
    |          | `headers`     | Map<String, String\>              |            |

=== "Outputs"
    No output. Only a log in report if message was successfully sent


### Example

=== "JMS"
    ``` kotlin
    JmsSenderAction(
        target = "JMS_TARGET",
        destination = "jms/domain/my/queue",
        body = "my text body"
        attributes = mapOf(
            "jms.MyProperty" to "some value"
        )
    )
    ```
=== "Jakarta"
    ``` kotlin
    JakartaSenderAction(
        target = "JMS_TARGET",
        destination = "jms/domain/my/queue",
        body = "my text body"
        attributes = mapOf(
            "jms.MyProperty" to "some value"
        )
    )
    ```

# Listener
!!! info "Browse [JMS](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jms/JmsListenerAction.java){:target="_blank"} or [Jakarta](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jakarta/JakartaListenerAction.java){:target="_blank"} implementation"

*  **Only works on TextMessage**
* `selector` used as message filter in **createConsumer** ([JMS](https://docs.oracle.com/javaee/7/api/javax/jms/Session.html#createConsumer-javax.jms.Destination-java.lang.String-){:target="_blank"} / [Jakarta](https://jakarta.ee/specifications/messaging/3.1/apidocs/jakarta.messaging/jakarta/jms/session#createConsumer(jakarta.jms.Destination,java.lang.String)){:target="_blank"}) or in **createBrowser** ([JMS](https://docs.oracle.com/javaee/7/api/javax/jms/Session.html#createBrowser-javax.jms.Queue-java.lang.String-){:target="_blank"} / [Jakarta](https://jakarta.ee/specifications/messaging/3.1/apidocs/jakarta.messaging/jakarta/jms/session#createBrowser(jakarta.jms.Queue,java.lang.String)){:target="_blank"})
* `bodySelector` verify in `browserMaxDepth` messages on the queue if it contains `bodySelector` characters

=== "Inputs"

    | Required | Name              | Type                                                                       | Default |
    |:--------:|:------------------|:---------------------------------------------------------------------------|:-------:|
    |    *     | `target`          | String                                                                     |         |
    |    *     | `destination`     | String                                                                     |         |
    |          | `selector`        | String                                                                     |         |
    |          | `bodySelector`    | String                                                                     |         |
    |          | `browserMaxDepth` | Integer                                                                    |         |
    |          | `timeOut`         | [Duration](/documentation/actions/introduction.md/#duration-type) (String) | 500 ms  |

=== "Outputs"

    |            Name | Type                |
    |----------------:|:--------------------|
    |   `textMessage` | String              |
    | `jmsProperties` | Map<String, Object> |

### Example

=== "JMS"
    ``` kotlin
    JmsListenerAction(
        target = "JMS_TARGET",
        destination = "jms/domain/my/queue",
        selector = "type = 'boat' AND color = 'red'",
        bodySelector = "some value to search in message",
        browserMaxDepth = 100,
        timeOut = "1 s"
    )
    ```
=== "Jakarta"
    ``` kotlin
    JakartaListenerAction(
        target = "JMS_TARGET",
        destination = "jms/domain/my/queue",
        selector = "type = 'boat' AND color = 'red'",
        bodySelector = "some value to search in message",
        browserMaxDepth = 100,
        timeOut = "1 s"
    )
    ```

# Clean Queue
!!! info "Browse [JMS](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jms/JmsCleanQueueAction.java){:target="_blank"} or [Jakarta](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/fr/enedis/chutney/action/jakarta/JakartaCleanQueueAction.java){:target="_blank"} implementation"

* `selector` used as message filter in **createConsumer** ([JMS](https://docs.oracle.com/javaee/7/api/javax/jms/Session.html#createConsumer-javax.jms.Destination-java.lang.String-){:target="_blank"} / [Jakarta](https://jakarta.ee/specifications/messaging/3.1/apidocs/jakarta.messaging/jakarta/jms/session#createConsumer(jakarta.jms.Destination,java.lang.String)){:target="_blank"}) or in **createBrowser** ([JMS](https://docs.oracle.com/javaee/7/api/javax/jms/Session.html#createBrowser-javax.jms.Queue-java.lang.String-){:target="_blank"} / [Jakarta](https://jakarta.ee/specifications/messaging/3.1/apidocs/jakarta.messaging/jakarta/jms/session#createBrowser(jakarta.jms.Queue,java.lang.String)){:target="_blank"})
* `bodySelector` verify in `browserMaxDepth` messages on the queue if it contains `bodySelector` characters **(only works on TextMessage)**

=== "Inputs"

    | Required | Name              | Type                                                                       | Default |
    |:--------:|:------------------|:---------------------------------------------------------------------------|:-------:|
    |    *     | `target`          | String                                                                     |         |
    |    *     | `destination`     | String                                                                     |         |
    |          | `selector`        | String                                                                     |         |
    |          | `bodySelector`    | String                                                                     |         |
    |          | `browserMaxDepth` | Integer                                                                    |         |
    |          | `timeOut`         | [Duration](/documentation/actions/introduction.md/#duration-type) (String) | 500 ms  |

No output. Only a log in report with number of messages removed

### Example

=== "JMS"
    ``` kotlin
    JmsCleanQueueAction(
        target = "JMS_TARGET",
        destination = "jms/domain/my/queue",
        selector = "type = 'boat' AND color = 'red'",
        bodySelector = "some value to search in message",
        browserMaxDepth = 100,
        timeOut = "1 s"
    )
    ```
=== "Jakarta"
    ``` kotlin
    JakartaCleanQueueAction(
        target = "JMS_TARGET",
        destination = "jms/domain/my/queue",
        selector = "type = 'boat' AND color = 'red'",
        bodySelector = "some value to search in message",
        browserMaxDepth = 100,
        timeOut = "1 s"
    )
    ```
