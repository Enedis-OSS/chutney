#SPDX-FileCopyrightText: 2017-2024 Enedis
#SPDX-License-Identifier: Apache-2.0

FROM eclipse-temurin:21-jre
VOLUME /config
VOLUME /.chutney
ARG JAR_PATH=packaging/local-dev/target
COPY $JAR_PATH/local-dev-*.jar app.jar
LABEL org.opencontainers.image.source https://github.com/Enedis-OSS/chutney
EXPOSE 8443
CMD java -jar /app.jar
