<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

If you want to have a quick tour of Chutney, a [demonstration container](https://github.com/Enedis-OSS/chutney/blob/a55aadce85de6fa99c0d653286200a16bfdf1176/example/.docker/dev-docker-compose-demo.yml){:target="_blank"} could be launched locally.

!!! note "Launch demonstration container"

    1. Checkout [chutney](https://github.com/Enedis-OSS/chutney){:target="_blank"}
    2. From the **example** directory, execute the command
    ``` sh
        docker compose -f ./.docker/dev-docker-compose-demo.yml up -d
    ```
    3. Access the web interface in your browser with url [http://localhost](http://localhost){:target="_blank"}
    4. Log in with one of predefined user : **reader**, **editor**, **executor** or **admin**, with associated password like the user name capitalized
    5. Browse at your will the interface !!

# What's in

This demo Chutney instance has some scenarios and associated campaigns, showing SQL, JSON and XML processing, which could be played with.

## SQL examples

The service tested is Chutney itself.  
Two scenarios are defined :

 * Check that there is some scenarios to execute
 * Check that last execution of every defined scenario is in success

A campaign grouping these two scenarios is defined.

## JSON examples

The service tested is [SWAPI](https://swapi.dev/api){:target="_blank"}.  
Two scenarios are defined :

 * Check that the root api list all available resources
 * Check that all people resources are valid against a local schema

A campaign grouping these two scenarios is defined.

## XML examples

The service tested is [ARXIV](http://export.arxiv.org/api){:target="_blank"}.  
Two scenarios are defined :

 * Check that there is some articles about software testing
 * Check that search response is valid against a local atom feed schema

A campaign grouping these two scenarios is defined.

# Supervision bonus

Besides the Chutney server service, the demonstration container starts two others services, which demonstrates how to do a supervision :

 * A monitoring system with time series which collects the Chutney server metrics. We chose [Prometheus](https://prometheus.io/){:target="_blank"} for this demo
 * An observability system with three sample dashboards. We chose [Grafana](https://grafana.com/){:target="_blank"} for this demo

!!! note "Access demonstration container supervision"

    1. Access the web interface in your browser with url [http://localhost:3000](http://localhost:3000){:target="_blank"}
    2. Log in as admin/admin and choose a new admin password
    3. Choose one of three dashboards available at [http://localhost:3000/dashboards](http://localhost:3000/dashboards){:target="_blank"}
    4. Additionnaly, the prometheus time series are shown at [http://localhost:9090/tsdb-status](http://localhost:9090/tsdb-status){:target="_blank"}
