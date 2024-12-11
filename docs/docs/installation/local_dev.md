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
<!-- Wait for project template
!!! note "Docker"

    1. Checkout this [example project](https://github.com/Enedis-OSS/chutney-project-template)
    2. Start Chutney locally with `docker compose up&` ([Docker compose documentation](https://docs.docker.com/compose/)).
-->

!!! note "Docker"

    1. Start Chutney locally with `docker -p 8443:8443 -d run ghcr.io/chutney-testing/chutney/chutney-server` ([Docker run documentation](https://docs.docker.com/reference/cli/docker/container/run/)).

!!! note "Java"

    1. Download the latest release jar [chutney-local-dev-x.x.x.jar](https://github.com/Enedis-OSS/chutney/releases/latest).
    2. Start Chutney locally with `java -jar chutney-local-dev-x.x.x.jar`

!!! note "Intellij"

    1. Checkout [chutney](https://github.com/Enedis-OSS/chutney).
    2. Build the project using maven : `mvn compile [-DuseExternalNpm]`
    3. Start [Intellij run configuration](https://www.jetbrains.com/help/idea/run-debug-configuration.html) `start_local_server`

