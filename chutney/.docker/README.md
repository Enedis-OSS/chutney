<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
  -->
### Build image

From project root folder, run:

```shell
docker build --tag ghcr.io/enedis-oss/chutney/chutney-server:latest . -f ./.docker/Dockerfile
```
### Start container

```shell
docker run -d \
  --name chutney-server \
  -p 443:8443 \
  -v ./.chutney/:/.chutney \
  --restart unless-stopped \
  ghcr.io/enedis-oss/chutney/chutney-server

```

**Notes :**

* Server container will run with default configuration(see server module)
* It's possible to override default configuration like any other spring boot application when running server container (see [this](https://github.com/Enedis-OSS/chutney/blob/main/example/.docker/dev-docker-compose-demo.yml){:target="_blank"} compose file for example)

### Enjoy app

visit https://localhost:8443
```
