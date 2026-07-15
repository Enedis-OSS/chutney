/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http.domain;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public interface HttpClient {

    ResponseEntity<String> call(HttpMethod httpMethod, String resource, HttpEntity<?> input) throws HttpClientErrorException;

    default ResponseEntity<String> post(String resource, Object body, HttpHeaders headers) throws HttpClientErrorException {
        return call(HttpMethod.POST, resource, new HttpEntity<>(body, headers));
    }

    default ResponseEntity<String> put(String resource, Object body, HttpHeaders headers) throws HttpClientErrorException {
        return call(HttpMethod.PUT, resource, new HttpEntity<>(body, headers));
    }

    default ResponseEntity<String> get(String resource, HttpHeaders headers) throws HttpClientErrorException {
        return call(HttpMethod.GET, resource, new HttpEntity<>(null, headers));
    }

    default ResponseEntity<String> delete(String resource, HttpHeaders headers) throws HttpClientErrorException {
        return call(HttpMethod.DELETE, resource, new HttpEntity<>(null, headers));
    }

    default ResponseEntity<String> patch(String resource, Object body, HttpHeaders headers) throws HttpClientErrorException {
        return call(HttpMethod.PATCH, resource, new HttpEntity<>(body, headers));
    }
}
