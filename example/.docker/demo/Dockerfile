#SPDX-FileCopyrightText: 2017-2024 Enedis
#SPDX-License-Identifier: Apache-2.0

FROM ghcr.io/enedis-oss/chutney/chutney-server

LABEL org.opencontainers.image.source https://github.com/Enedis-OSS/chutney

ARG CONF=.docker/demo
COPY $CONF/conf /.chutney
COPY $CONF/spring-config /config
COPY $CONF/schema /schema
ENV SPRING_PROFILES_ACTIVE=mem-auth,db-sqlite-rw
ENV SPRING_CONFIG_LOCATION=file:/config/
ENV LOADER_PATH=/schema/*
