<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->


You can run scenarios without installing a Chutney server. This fits well within a CI or for a developer local setup.

However, building software is most often a teamwork !  
Doing so, you will need to collaborate and share scenarios, track their executions 
and allow functional and business analyst to review and be involved in testing their product.

That's why we provide a server and web UI to help us do all these things.

<!-- Wait for project template
You can find all code and configuration below in this [example project](https://github.com/Enedis-OSS/chutney-project-template){:target="_blank"}
-->

# Start a server

!!! note "Maven"

    1. Checkout [chutney](https://github.com/Enedis-OSS/chutney).
    2. Go to server module `cd chutney/server`
    3. Start Chutney locally with `mvn spring-boot:run`

!!! note "Intellij"

    1. Checkout [chutney](https://github.com/Enedis-OSS/chutney).
    2. Start [Intellij run configuration](https://www.jetbrains.com/help/idea/run-debug-configuration.html) `start_local_server`

!!! note "Boot jar"

    1. Download chutney-server-<version>.jar from last relase [assets](https://github.com/Enedis-OSS/chutney/releases){:target="_blank"}.   
    2. Run server jar: `java -jar chutney-server-<version>.jar`
