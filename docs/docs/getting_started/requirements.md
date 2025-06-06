<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

# Introduction

With Chutney you can write and run functional scenarios.  
Those scenarios validate high level requirements and are not tied to your application code.

Instead, you will target I/O interfaces of your application (i.e. HTTP endpoints, Kafka topics, AMQP queues, etc.)
without writing the same boilerplate code for respective clients, consumers, or even mock clients or producers.

To get you started, we will cover how to write scenarios with the [Chutney Kotlin DSL](https://github.com/Enedis-OSS/chutney/tree/main/kotlin-dsl){:target="_blank"}
and run them with JUnit5.

# Minimal Setup

!!! important "Requirements"

    * `java` 17 or later and Kotlin
    * your preferred build tool (ex. `maven`, `gradle`, etc.)
    * your preferred test engine (ex. `Junit 5.x`, `TestNG`, etc.)

??? note "Building a Kotlin project"

    In order to build your Kotlin project, you may need to add the following configuration with maven :

    === "maven"

        ``` xml
        <build>
            <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>
            <testSourceDirectory>${project.basedir}/src/test/kotlin</testSourceDirectory>

            <plugins>
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>
                    <version>1.9.21</version>
                    <executions>
                        <execution>
                            <id>compile</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>compile</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>test-compile</id>
                            <goals>
                                <goal>test-compile</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
        ```

# Dependencies

Create a Kotlin project with the following dependencies :

* [fr.enedis.chutney:chutney-kotlin-dsl](https://search.maven.org/artifact/fr.enedis.chutney/chutney-kotlin-dsl){:target="_blank"}
* [org.jetbrains.kotlin:kotlin-stdlib](https://search.maven.org/artifact/org.jetbrains.kotlin/kotlin-stdlib){:target="_blank"}
* [org.junit.jupiter:junit-jupiter-api](https://search.maven.org/artifact/org.junit.jupiter/junit-jupiter-api){:target="_blank"}

=== "maven"

    ``` xml
    <dependencies>
        <dependency>
            <groupId>fr.enedis.chutney</groupId>
            <artifactId>chutney-kotlin-dsl</artifactId> <!--(1)-->
            <version>3.0.0</version>
        </dependency>
       <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId> <!--(2)-->
            <version>1.9.21</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId> <!-- Optional (3) -->
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    ```

    1. Required for using the Chutney Kotlin DSL
    2. Required for compiling Kotlin project
    3. Only required if you want to run your test within IntelliJ with the [gutter icon](https://www.jetbrains.com/help/idea/settings-gutter-icons.html){:target="_blank"} :fontawesome-regular-circle-play:

=== "gradle"

    ``` kotlin
    dependencies {
        implementation("fr.enedis.chutney:chutney-kotlin-dsl:3.0.0")

        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    }
    ```
