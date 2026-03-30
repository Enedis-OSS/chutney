/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */


plugins {
  id("java") // Java support
  alias(libs.plugins.kotlin) // Kotlin support
  alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
}

private val chutneyVersion = providers.gradleProperty("chutneyVersion").get()

group = providers.gradleProperty("pluginGroup")
version = chutneyVersion

configurations.all {
  exclude("org.sl4j")
  resolutionStrategy {
    eachDependency {
      if (requested.group == "org.jetbrains.kotlin") {
        useVersion("2.3.20")
        because("Gradle 9.4.1 uses Kotlin build-tools 2.3.20 internally")
      }
    }
    //failOnVersionConflict()

  }
}

/*configurations.named("runtimeOnly") {
  shouldResolveConsistentlyWith(configurations.getByName("implementation"))
}*/

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  intellijPlatform {
    // Minimal compatible IntelliJ version
    intellijIdea(providers.gradleProperty("platformVersion"))
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.plugins.yaml")
    bundledPlugin("com.intellij.spring")
    bundledPlugin("org.jetbrains.kotlin")

    pluginVerifier()
  }

  implementation(enforcedPlatform("fr.enedis.chutney:chutney-parent:$chutneyVersion"))
  implementation("fr.enedis.chutney", "chutney-kotlin-dsl", chutneyVersion) {
    isTransitive = false
  }

  runtimeOnly("com.fasterxml.jackson.module", "jackson-module-kotlin")
  runtimeOnly("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
  runtimeOnly("com.fasterxml.jackson.module", "jackson-module-paranamer")

  runtimeOnly("org.apache.httpcomponents.client5", "httpclient5") {
    exclude("org.slf4j")
  }
  runtimeOnly("org.apache.httpcomponents.core5", "httpcore5")

  implementation("com.google.guava", "guava")
  implementation("org.hjson", "hjson")
  implementation("org.apache.commons", "commons-text")
  implementation("com.fasterxml.jackson.core", "jackson-core")
  implementation("com.fasterxml.jackson.core", "jackson-databind")
  implementation("com.fasterxml.jackson.core", "jackson-annotations")
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml")

  implementation("org.jetbrains:annotations") {
    version { strictly("24.0.0") }
  }

  implementation("org.jetbrains.kotlin:kotlin-script-util:1.8.22")

  implementation("me.andrz.jackson", "jackson-json-reference-core", "0.3.2") {
    isTransitive = false
  }

  runtimeOnly("fr.enedis.chutney", "chutney-server", chutneyVersion, ext = "jar") {
    isTransitive = false
    artifact { classifier = "boot" }
  }
}

intellijPlatform {
  buildSearchableOptions = false

  pluginConfiguration {
    version = chutneyVersion
    ideaVersion {
      sinceBuild = providers.gradleProperty("pluginSinceBuild")
    }
  }

  publishing {
    token = System.getenv("PUBLISH_TOKEN")
    channels = listOf(
      chutneyVersion.split('-')
        .getOrElse(1) { "default" }
        .split('.')
        .first()
    )
  }

  pluginVerification {
    ides {
      recommended()
    }
  }
}