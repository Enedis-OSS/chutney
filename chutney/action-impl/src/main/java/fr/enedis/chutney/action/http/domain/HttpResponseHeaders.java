/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/**
 * HTTP response headers, as exposed to scenarios through the {@code headers} action output.
 *
 * <p>Until Spring Framework 7, {@link HttpHeaders} implemented {@link MultiValueMap}. Scenarios could
 * therefore both index headers by name, as in {@code ${#headers['set-cookie'][0]}}, and call the typed
 * getters, as in {@code ${#headers.getContentType()}}. Spring 7 removed that interface: indexing then
 * failed with {@code EL1027E} and execution reports serialized headers as a bean rather than a map.
 *
 * <p>This type restores both usages by extending {@link HttpHeaders} and implementing the {@link Map}
 * contract on top of {@link HttpHeaders#headerSet()} and {@link HttpHeaders#headerNames()}, which are
 * case-insensitive views over the headers Spring already holds. Lookup therefore stays case-insensitive:
 * {@code #headers['Set-Cookie']} and {@code #headers['set-cookie']} resolve alike.
 *
 * <p>Map access is the preferred form. The typed getters are kept only so that scenarios written before
 * the Spring 7 upgrade keep working, and are expected to be dropped once a major version allows it.
 */
public class HttpResponseHeaders extends HttpHeaders implements MultiValueMap<String, String> {

    public HttpResponseHeaders(HttpHeaders headers) {
        super(headers);
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof String name && containsHeader(name);
    }

    @Override
    public boolean containsValue(Object value) {
        return headerSet().stream().anyMatch(header -> header.getValue().equals(value));
    }

    @Override
    public List<String> get(Object key) {
        return key instanceof String name ? super.get(name) : null;
    }

    @Override
    public List<String> remove(Object key) {
        return key instanceof String name ? super.remove(name) : null;
    }

    @Override
    public Set<String> keySet() {
        return headerNames();
    }

    @Override
    public Collection<List<String>> values() {
        return headerSet().stream().map(Map.Entry::getValue).toList();
    }

    @Override
    public Set<Map.Entry<String, List<String>>> entrySet() {
        return headerSet();
    }

    @Override
    public void addAll(MultiValueMap<String, String> other) {
        other.forEach(super::addAll);
    }
}
