/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.infra;

import com.atlassian.httpclient.api.Request;
import com.atlassian.httpclient.api.factory.Scheme;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import fr.enedis.chutney.jira.domain.JiraServerConfiguration;
import fr.enedis.chutney.jira.domain.JiraXrayApi;
import fr.enedis.chutney.jira.domain.exception.NoJiraConfigurationException;
import fr.enedis.chutney.jira.infra.atlassian.httpclient.api.factory.HttpClientOptions;
import fr.enedis.chutney.jira.infra.atlassian.httpclient.api.factory.ProxyOptions;
import fr.enedis.chutney.jira.infra.atlassian.jira.rest.client.internal.async.AsynchronousHttpClientFactory;
import fr.enedis.chutney.jira.xrayapi.JiraIssueType;
import fr.enedis.chutney.jira.xrayapi.Xray;
import fr.enedis.chutney.jira.xrayapi.XrayTestExecTest;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class HttpJiraXrayImpl implements JiraXrayApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpJiraXrayImpl.class);

    private static final int MS_TIMEOUT = 10 * 1000; // 10 s

    private final JiraServerConfiguration jiraServerConfiguration;

    public HttpJiraXrayImpl(JiraServerConfiguration jiraServerConfiguration) {
        this.jiraServerConfiguration = jiraServerConfiguration;
        if (!jiraServerConfiguration.isValid()) {
            throw new NoJiraConfigurationException();
        }
    }

    @Override
    public void updateRequest(Xray xray) {
        String updateUri = jiraServerConfiguration.url() + "/rest/raven/1.0/import/execution";

        RestTemplate restTemplate = buildRestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(updateUri, xray, String.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                LOGGER.debug(response.toString());
                LOGGER.info("Xray successfully updated for " + xray.getTestExecutionKey());
            } else {
                LOGGER.error(response.toString());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Unable to update test execution [" + xray.getTestExecutionKey() + "] : ", e);
        }
    }

    @Override
    public List<XrayTestExecTest> getTestExecutionScenarios(String xrayId) {
        List<XrayTestExecTest> tests = new ArrayList<>();

        String uriTemplate = jiraServerConfiguration.url() + "/rest/raven/1.0/api/%s/%s/test";
        String uri = String.format(uriTemplate, isTestPlan(xrayId) ? "testplan" : "testexec", xrayId);

        RestTemplate restTemplate = buildRestTemplate();
        try {
            ResponseEntity<XrayTestExecTest[]> response = restTemplate.getForEntity(uri, XrayTestExecTest[].class);
            if (response.getStatusCode().equals(HttpStatus.OK) && response.getBody() != null) {
                tests = Arrays.stream(response.getBody())
                    .toList();
            } else {
                LOGGER.error(response.toString());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Unable to get xray test execution[" + xrayId + "] scenarios : ", e);
        }
        return tests;
    }

    @Override
    public void updateStatusByTestRunId(String testRuntId, String executionStatus) {
        String uriTemplate = jiraServerConfiguration.url() + "/rest/raven/1.0/api/testrun/%s/status?status=%s";
        String uri = String.format(uriTemplate, testRuntId, executionStatus);

        RestTemplate restTemplate = buildRestTemplate();
        try {
            restTemplate.put(uri, null);
        } catch (RestClientException e) {
            throw new RuntimeException("Unable to update xray testRuntId[" + testRuntId + "] with status[" + executionStatus + "] : ", e);
        }
    }

    @Override
    public void associateTestExecutionFromTestPlan(String testPlanId, String testExecutionId) {
        String uriTemplate = jiraServerConfiguration.url() + "/rest/raven/1.0/api/testplan/%s/testexecution";
        String uri = String.format(uriTemplate, testPlanId);

        RestTemplate restTemplate = buildRestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uri, Map.of("add", List.of(testExecutionId)), String.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                LOGGER.debug(response.toString());
                LOGGER.info("Xray successfully associate test execution [" + testExecutionId + "] from test plan [" + testPlanId + "]");
            } else {
                LOGGER.error(response.toString());
                throw new RuntimeException("Unable to associate test execution [" + testExecutionId + "] from test plan [" + testPlanId + "]");
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Unable to associate test execution [" + testExecutionId + "] from test plan [" + testPlanId + "] : ", e);
        }
    }

    @Override
    public String createTestExecution(String testPlanId) {
        Issue parentIssue = getIssue(testPlanId);
        IssueInputBuilder issueInputBuilder = new IssueInputBuilder();

        IssueInput issueInput = issueInputBuilder
            .setProjectKey(parentIssue.getProject().getKey())
            .setIssueTypeId(getIssueTypeByName("Test Execution").getId())
            .setSummary(parentIssue.getSummary())
            .build();

        try (JiraRestClient jiraRestClient = getJiraRestClient()) {
            BasicIssue issue = jiraRestClient.getIssueClient().createIssue(issueInput).claim();
            associateTestExecutionFromTestPlan(testPlanId, issue.getKey());
            return issue.getKey();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create test execution issue from test plan [" + testPlanId + "] : ", e);
        }
    }

    @Override
    public boolean isTestPlan(String issueId) {
        if (issueId.isEmpty()) {
            return false;
        }
        return getIssue(issueId).getIssueType().getId().equals(getIssueTypeByName("Test Plan").getId());
    }

    private Issue getIssue(String issueKey) {
        try (JiraRestClient jiraRestClient = getJiraRestClient()) {
            return jiraRestClient.getIssueClient().getIssue(issueKey).claim();
        } catch (Exception e) {
            throw new RuntimeException("Unable to get issue [" + issueKey + "] : ", e);
        }
    }

    private JiraIssueType getIssueTypeByName(String issueTypeName) {
        String uri = jiraServerConfiguration.url() + "/rest/api/latest/issuetype";
        Optional<JiraIssueType> issueTypeOptional = Optional.empty();

        RestTemplate restTemplate = buildRestTemplate();
        try {
            ResponseEntity<JiraIssueType[]> response = restTemplate.getForEntity(uri, JiraIssueType[].class);
            if (response.getStatusCode().equals(HttpStatus.OK) && response.getBody() != null) {
                issueTypeOptional = Arrays.stream(response.getBody())
                    .filter(issueType -> issueType.getName().equals(issueTypeName))
                    .findFirst();
            } else {
                LOGGER.error(response.toString());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Unable to get issues type list : ", e);
        }
        return issueTypeOptional.orElseThrow(() -> new RuntimeException("Unable to get issue type [" + issueTypeName + "]"));
    }

    private RestTemplate buildRestTemplate() {
        try {
            var requestFactory = new HttpComponentsClientHttpRequestFactory(buildHttpClient());
            requestFactory.setConnectTimeout(MS_TIMEOUT);
            return new RestTemplate(requestFactory);
        } catch (Exception e) {
            throw new RuntimeException("Cannot build rest template.", e);
        }
    }

    private HttpClient buildHttpClient() throws URISyntaxException {
        HttpHost httpHost = HttpHost.create(new URI(jiraServerConfiguration.url()));
        HttpHost proxyHttpHost = null;
        if (!jiraServerConfiguration.urlProxy().isBlank()) {
            proxyHttpHost = HttpHost.create(new URI(jiraServerConfiguration.urlProxy()));
        }

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
            .setConnectionManager(buildConnectionManager())
            .setDefaultCredentialsProvider(getBasicCredentialsProvider(httpHost, proxyHttpHost));

        var defaultHeaders = new ArrayList<BasicHeader>();
        String authorization = basicAuthHeaderEncodedValue(jiraServerConfiguration.username(), jiraServerConfiguration.password());
        defaultHeaders.add(new BasicHeader(HttpHeaders.AUTHORIZATION, authorization));

        if (jiraServerConfiguration.hasProxy()) {
            httpClientBuilder.setProxy(proxyHttpHost);
            if (jiraServerConfiguration.hasProxyWithAuth()) {
                String proxyAuthorization = basicAuthHeaderEncodedValue(jiraServerConfiguration.userProxy(), jiraServerConfiguration.passwordProxy());
                defaultHeaders.add(new BasicHeader(HttpHeaders.PROXY_AUTHORIZATION, proxyAuthorization));
            }
        }

        httpClientBuilder.setDefaultHeaders(defaultHeaders);

        return httpClientBuilder.build();
    }

    private HttpClientConnectionManager buildConnectionManager() {
        SSLContext sslContext = buildSslContext();
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        return PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(socketFactory)
            .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(MS_TIMEOUT, TimeUnit.MILLISECONDS).build())
            .build();
    }

    private SSLContext buildSslContext() {
        try {
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            sslContextBuilder.loadTrustMaterial(null, (chain, authType) -> true);
            return sslContextBuilder.build();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String basicAuthHeaderEncodedValue(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    private BasicCredentialsProvider getBasicCredentialsProvider(HttpHost httpHost, HttpHost proxyHttpHost) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            new AuthScope(httpHost),
            new UsernamePasswordCredentials(jiraServerConfiguration.username(), jiraServerConfiguration.password().toCharArray())
        );
        if (jiraServerConfiguration.hasProxyWithAuth()) {
            credentialsProvider.setCredentials(
                new AuthScope(proxyHttpHost),
                new UsernamePasswordCredentials(jiraServerConfiguration.userProxy(), jiraServerConfiguration.passwordProxy().toCharArray())
            );
        }
        return credentialsProvider;
    }

    private JiraRestClient getJiraRestClient() throws URISyntaxException {
        URI serverUri = URI.create(jiraServerConfiguration.url());
        return new AsynchronousJiraRestClient(
            serverUri,
            buildJiraHttpClient(
                serverUri,
                new BasicHttpAuthenticationHandler(
                    jiraServerConfiguration.username(), jiraServerConfiguration.password(),
                    jiraServerConfiguration.userProxy(), jiraServerConfiguration.passwordProxy()
                )
            )
        );
    }

    private DisposableHttpClient buildJiraHttpClient(
        final URI serverUri,
        final AuthenticationHandler authenticationHandler
    ) throws URISyntaxException {
        final HttpClientOptions options = new HttpClientOptions();
        options.setTrustSelfSignedCertificates(true);

        if (jiraServerConfiguration.hasProxy()) {
            HttpHost proxyHttpHost = HttpHost.create(new URI(jiraServerConfiguration.urlProxy()));
            options.setProxyOptions(
                ProxyOptions.ProxyOptionsBuilder.create()
                    .withProxy(Scheme.valueOf(serverUri.getScheme().toUpperCase()), proxyHttpHost)
                    .build()
            );
        }

        return AsynchronousHttpClientFactory.createClient(
            serverUri, authenticationHandler, options
        );
    }

    private record BasicHttpAuthenticationHandler(
        String username,
        String password,
        String proxyUsername,
        String proxyPassword
    ) implements AuthenticationHandler {
        @Override
        public void configure(Request.Builder builder) {
            builder.setHeader(HttpHeaders.AUTHORIZATION, basicAuthHeaderEncodedValue(username, password));
            if (proxyUsername != null && !proxyUsername.isBlank()) {
                builder.setHeader(HttpHeaders.PROXY_AUTHORIZATION, basicAuthHeaderEncodedValue(proxyUsername, proxyPassword));
            }
        }
    }
}
