#SPDX-FileCopyrightText: 2017-2024 Enedis
#SPDX-License-Identifier: Apache-2.0

FROM eclipse-temurin:21-jre

VOLUME /.chutney
EXPOSE 8443

LABEL org.opencontainers.image.source=https://github.com/Enedis-OSS/chutney

ARG JAR_PATH=server/target
COPY $JAR_PATH/chutney-server-*-boot.jar app.jar

CMD ["java", "-jar", "/app.jar", "--server.port=8443", "--chutney.workspace=/.chutney"]
