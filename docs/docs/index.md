<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

[![Build](https://github.com/Enedis-OSS/chutney/actions/workflows/build-all.yml/badge.svg?branch=main)](https://github.com/Enedis-OSS/chutney/actions/workflows/build-all.yml){:target="_blank"}
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.enedis.chutney/server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.enedis.chutney/server){:target="_blank"}
[![GitHub release](https://img.shields.io/github/v/release/Enedis-OSS/chutney)](https://github.com/Enedis-OSS/chutney/releases/latest){:target="_blank"}
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

----
**Spice up your spec , Better `taste` your app !**

Chutney helps you test deployed applications and validate functional requirements.  
Chutney provides you a set of capabilities for executing your scenario without having to write and maintain glue code.  

:material-rocket-launch: Install, configure, write and run your first scenario [here](/getting_started/requirements.md) !  
:octicons-book-16: You can find out a comprehensive list of what Chutney can do in the [documentation](/documentation/actions/introduction.md).

----

# Why another test tool ?

Chutney is an opinionated test tool based upon the practice of Specification by Example.

Chutney was inspired by Seb Rose blog post in which he revised the test pyramid according to test readability
[The Testing Iceberg](http://claysnow.co.uk/the-testing-iceberg/){:target="_blank"}

Chutney is not exactly what Seb Rose meant by using this metaphor.

But we envisioned a tool allowing multiple levels of readability, providing a single place for business people,
testers and developers to co-create, share and execute acceptance tests.

Moreover, we needed to :

* Promote and support Specification by Example across multiple teams and offices
* Ease collaboration and shared understanding in a "not so agile" environment
* Provide a single source of truth without hiding details in tests _glue code_
* Ease the automation of thousands of manual tests without writing and maintaining specific code
* Automate end-to-end tests of distributed software across secured networks, including hardware over telco networks

# What it is not

__Chutney is not a replacement for tools like Cucumber, etc.__

While having some overlap, they all fill different test aspect.

The key difference is the absence of glue and support code.

While we think that having glue code is cumbersome and adds unnecessary levels of indirection between the features and the system under test,
especially for high level tests and distributed software.

We also do think that using Cucumber for low level testing is sometimes very handy and useful,
thanks to the high level of expression provided by Gherkin (and this is part of the Testing Iceberg Seb Rose talked about).


__Chutney is no silver-bullet, it is just a tool which promotes and supports one way of doing software testing.__

As such, to benefit from it, we highly advise you to be proficient or to document yourself about
Behaviour-Driven-Development (by Dan North), Specification by Example (by Gojko Adzic) and Living Documentation (by Cyrille Martraire).
All of which, however you call it, define the same practices and share the same goals.

Global understanding of Test Driven Development and knowledge about Ubiquitous Language (from Domain Driven Design, by Eric Evans)
is also valuable.

# Why Choose Chutney ?

Chutney stands out for its simple and flexible approach to test automation, focusing on complex scenarios and end-to-end testing. Compared to other similar tools like [Cucumber](https://cucumber.io/){:target="_blank"} or [Karate](https://karatelabs.github.io/karate/){:target="_blank"}, here are some key points :

 * **Simplified DSL**  
   Chutney offers a Kotlin-based Domain-Specific Language (DSL) that allows writing readable and easy-to-maintain scenarios for developers. This is an attractive alternative for teams preferring a more structured language than the one used by Cucumber with regexp.

 * **Focused on blackbox Testing**  
   Unlike Cucumber, which is often used for functional testing at build, Chutney is specifically designed for testing on deployed application. It excels at handling API calls, database interactions, and verifying asynchronous call chains.

 * **User-Friendly Interface for Non-Developers**  
   Chutney includes an interface specifically designed for non-developers, enabling them to execute tests, plan test campaigns, and analyze reports with ease. This sets it apart from other tools like Cucumber or Karate, which often require technical expertise for these tasks. This accessibility bridges the gap between technical and non-technical teams, fostering collaboration and improving efficiency.

 * **Multi-level Gherkin-like language**  
   Quoting the [Glacio](https://github.com/fridujo/glacio){:target="_blank"} project

    > There is a common problem encountered using BDD tools like Cucumber.
    > This is:
    >- Non-technical actors write specs
    >  - Developers write glue code containing hidden business specificities
      >  That is an historic clivage which tends to leave grey areas undiscovered until implementation time.
      >  Furthermore, non-technical actors can point out relevant aspects of implementation just as developers can do the same about the business part (aka specs).
      >  As Seb Rose stated in this article (https://claysnow.co.uk/the-testing-iceberg/), levels of readability can differ from the ones encountered in usual test segregation (unit, integration, acceptance, etc.).

    Chutney’s support for nested steps allows teams to write scenarios that reflect different levels of abstraction.High-level steps can describe functional requirements in a business-readable manner, while the underlying steps handle the technical details. This structured approach emphasizes functional clarity at the top layer, while efficiently managing technical complexity beneath, setting Chutney apart from tools like Cucumber or Karate, which often struggle to balance this granularity.

 
Finally, Chutney is production ready and used in companies by two main types of users

* Project Teams  
  These users leverage Chutney in a project-based mode to validate their developments. Typically, they use it at the end of the CI/CD pipeline to ensure their work meets integration and functional requirements after deployment.
* Cross-Application Testing Teams  
  Dedicated testing teams use Chutney across multiple interconnected applications. These teams focus on validating complex integrations and ensuring that end-to-end scenarios work seamlessly in large systems with various dependencies.

# Docker

Along each Chutney release, some built docker images are pushed to the [GitHub Chutney repo](https://github.com/orgs/Enedis-OSS/packages?repo_name=chutney){:target="_blank"} :

* [chutney-server](https://github.com/Enedis-OSS/chutney/pkgs/container/chutney%2Fchutney-server){:target="_blank"}  
  This [image](https://github.com/Enedis-OSS/chutney/blob/main/chutney/.docker/server/Dockerfile){:target="_blank"} contains the Chutney server instance with embedded ui.

* [chutney-ui](https://github.com/Enedis-OSS/chutney/pkgs/container/chutney%2Fchutney-ui){:target="_blank"}  
  This [image](https://github.com/Enedis-OSS/chutney/blob/main/chutney/.docker/ui/Dockerfile){:target="_blank"} contains only the Chutney ui.
    
* [chutney-demo](https://github.com/Enedis-OSS/chutney/pkgs/container/chutney%2Fchutney-demo){:target="_blank"}  
  This [image](https://github.com/Enedis-OSS/chutney/blob/main/example/.docker/demo/Dockerfile){:target="_blank"} contains a Chutney server with some scenarios and campaigns samples.  
  Check [this](/getting_started/demo.md) for more !!

!!! info "Details and compose"

    For more details information, check the server and ui [readme](https://github.com/Enedis-OSS/chutney/blob/main/chutney/.docker/README.md){:target="_blank"} and the demo [one](https://github.com/Enedis-OSS/chutney/blob/main/example/.docker/README.md){:target="_blank"}.  
    Some Docker compose files are also present in source code to play with.

??? tip "Legacy repo before 3.0.0"

    Checkout for this [legacy repo](https://github.com/orgs/chutney-testing/packages){:target="_blank"} for docker images before version 3.0.0.

# Front end

Chutney comes with a web application front end to launch and show executions of scenarios and campaigns.

*scenario execution report example*  
![swapi-ui-report.png](img/swapi-ui-report.png)

*campaign execution report example*  
![swapi-ui-campaign-report.png](img/swapi-ui-campaign-report.png)
