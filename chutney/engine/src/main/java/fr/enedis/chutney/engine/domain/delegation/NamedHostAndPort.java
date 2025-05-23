/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.delegation;

import java.util.Objects;
import java.util.Optional;

public class NamedHostAndPort {

    private final String name;
    private final String host;
    private final int port;

    public NamedHostAndPort(String name, String host, int port) {
        this.name = Optional.ofNullable(name).orElseThrow(() -> new IllegalArgumentException("Name should not be null"));
        this.host = Optional.ofNullable(host).orElseThrow(() -> new IllegalArgumentException("Host should not be null"));
        this.port = Optional.ofNullable(port).orElseThrow(() -> new IllegalArgumentException("Port should not be null"));
    }

    public String name() {
        return name;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    @Override
    public String toString() {
        return "name='" + name + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedHostAndPort that = (NamedHostAndPort) o;
        return port == that.port &&
            Objects.equals(name, that.name) &&
            Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, host, port);
    }
}
