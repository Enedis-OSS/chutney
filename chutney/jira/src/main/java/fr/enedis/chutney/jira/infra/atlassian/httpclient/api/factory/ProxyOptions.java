/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package fr.enedis.chutney.jira.infra.atlassian.httpclient.api.factory;

import com.atlassian.httpclient.api.factory.Scheme;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.hc.core5.http.HttpHost;
// CHANGE - End

/**
 * Contains proxy configuration for the HTTP client
 *
 * <pre>
 *  Changes :
 *   - Use Apache HttpHost's class
 *  </pre>
 * @see com.atlassian.httpclient.api.factory.ProxyOptions
 */
public class ProxyOptions {
    /**
     * Represents the mode of proxy configuration (i.e. from standard system properties (default), or configured
     * by the library consumer, or no proxy at all)
     */
    public enum ProxyMode {
        SYSTEM_PROPERTIES, CONFIGURED, NO_PROXY
    }

    /**
     * Get the mapping of schemes and their proxy hosts.
     *
     * @return the mapping of schemes and their proxy hosts.
     */
// CHANGE - Begin
    public Map<Scheme, HttpHost> getProxyHosts() {
// CHANGE - End
        return Collections.unmodifiableMap(proxyHostMap);
    }

    /**
     * @return the list of configured non-proxy hosts.
     */
    public Map<Scheme, List<String>> getNonProxyHosts() {
        return Collections.unmodifiableMap(nonProxyHosts);
    }

    /**
     * @return the mode of proxy configuration
     */
    public ProxyMode getProxyMode() {
        return proxyMode;
    }

    private final Map<Scheme, HttpHost> proxyHostMap;

    private final Map<Scheme, List<String>> nonProxyHosts;

    private final ProxyMode proxyMode;

// CHANGE - Begin
    private ProxyOptions(ProxyMode mode, Map<Scheme, HttpHost> proxyHostMap, Map<Scheme, List<String>> nonProxyHosts) {
// CHANGE - End
        this.proxyMode = mode;
        this.proxyHostMap = proxyHostMap;
        this.nonProxyHosts = nonProxyHosts;
    }

    /**
     * Use this builder to create a ProxyOptions
     */
    public static class ProxyOptionsBuilder {
// CHANGE - Begin
        private Map<Scheme, HttpHost> proxyHostMap = new HashMap<Scheme, HttpHost>();
// CHANGE - End

        private Map<Scheme, List<String>> nonProxyHosts = new HashMap<Scheme, List<String>>();

        private ProxyMode proxyMode = ProxyMode.SYSTEM_PROPERTIES;

        /**
         * Create a builder with default options (use what is configured in standard system properties (protocol).proxyHost/(protocol).proxyPort)
         *
         * @return Builder with default options set.
         */
        public static ProxyOptionsBuilder create() {
            return new ProxyOptionsBuilder();
        }

        /**
         * @return The proxy options from the builder settings
         */
        public ProxyOptions build() {
            return new ProxyOptions(proxyMode, proxyHostMap, nonProxyHosts);
        }

        /**
         * Use no proxy in the client
         *
         * @return Builder with 'no proxy' option set
         */
        public ProxyOptionsBuilder withNoProxy() {
            proxyHostMap = ImmutableMap.of();
            nonProxyHosts = ImmutableMap.of();
            proxyMode = ProxyMode.NO_PROXY;
            return this;
        }

        /**
         * Obtain proxy configuration for standard system properties (e.g. http.proxyHost, http.proxyPort, http.proxyUser, etc.)
         *
         * @return Builder with 'system properties' option set.
         */
        public ProxyOptionsBuilder withDefaultSystemProperties() {
            proxyHostMap = ImmutableMap.of();
            nonProxyHosts = ImmutableMap.of();
            proxyMode = ProxyMode.SYSTEM_PROPERTIES;
            return this;
        }

        /**
         * Add a proxy host for the given scheme. This enables 'configured' proxy mode, and will not obtain settings from
         * system properties any more.
         *
         * @param scheme    the scheme
         * @param proxyHost the proxy host
         * @return Builder with appropriate settings
         */
// CHANGE - Begin
        public ProxyOptionsBuilder withProxy(final @Nonnull Scheme scheme, final @Nonnull HttpHost proxyHost) {
// CHANGE - End
            Preconditions.checkNotNull(proxyHost, "Proxy host cannot be null");
            Preconditions.checkNotNull(scheme, "Scheme must not be null");

            this.proxyHostMap.put(scheme, proxyHost);
            proxyMode = ProxyMode.CONFIGURED;
            return this;
        }

        /**
         * Add a list of non-proxy hosts for the given scheme. This enables 'configured' proxy mode, and will not obtain settings from
         * system properties any more.
         *
         * @param scheme        The scheme
         * @param nonProxyHosts The list of non-proxy hosts
         * @return Builder with appropriate settings
         */
        public ProxyOptionsBuilder withNonProxyHost(final @Nonnull Scheme scheme, final @Nonnull List<String> nonProxyHosts) {
            Preconditions.checkNotNull(nonProxyHosts, "Non proxy hosts cannot be null");
            Preconditions.checkNotNull(scheme, "Scheme must not be null");

            // this does not impact whether we consider our mode to be - only proxy host/port can determine that
            this.nonProxyHosts.put(scheme, nonProxyHosts);
            return this;
        }

        /**
         * Configure proxies as per given arguments. This enables 'configured' proxy mode, and will not obtain settings from
         * system properties any more.
         *
         * @param proxyHostMap  Map of schemes to proxy hosts.
         * @param nonProxyHosts List of hosts that we shouldn't use the proxy for
         * @return Builder with appropriate settings
         */
// CHANGE - Begin
        public ProxyOptionsBuilder withProxy(Map<Scheme, HttpHost> proxyHostMap, Map<Scheme, List<String>> nonProxyHosts) {
// CHANGE - End
            this.proxyHostMap = proxyHostMap;
            this.nonProxyHosts = nonProxyHosts;
            proxyMode = ProxyMode.CONFIGURED;
            return this;
        }
    }

}
