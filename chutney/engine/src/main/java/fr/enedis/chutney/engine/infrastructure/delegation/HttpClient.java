/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.infrastructure.delegation;

import static fr.enedis.chutney.engine.api.execution.HttpTestEngine.EXECUTION_URL;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto;
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto;
import fr.enedis.chutney.engine.domain.delegation.CannotDelegateException;
import fr.enedis.chutney.engine.domain.delegation.ConnectionChecker;
import fr.enedis.chutney.engine.domain.delegation.DelegationClient;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import fr.enedis.chutney.engine.domain.execution.engine.Dataset;
import fr.enedis.chutney.engine.domain.execution.engine.Environment;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/* TODO all -
    An agent receiving a scenario fragment with a finally action will execute that teardown as soon as it finnish.
    The complete scenario will not work due to this early unexpected teardown.
    Thus, Finally Actions should be driven by the main Agent executing the whole scenario.
*/
public class HttpClient implements DelegationClient {

    private final RestTemplate restTemplate;
    private final ConnectionChecker connectionChecker;

    public HttpClient() {
        this(null, null);
    }

    public HttpClient(String username, String password) {
        this.restTemplate = new RestTemplate();
        this.connectionChecker = new TcpConnectionChecker();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.findAndRegisterModules();

        restTemplate.setMessageConverters(Lists.newArrayList(new MappingJackson2HttpMessageConverter(objectMapper)));
        addBasicAuth(username, password);
    }

    @Override
    public StepExecutionReport handDown(Step step, NamedHostAndPort delegate) throws CannotDelegateException {
        if (connectionChecker.canConnectTo(delegate)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Dataset dataset = new Dataset(emptyMap(), emptyList()); // TODO - check if it still works
            Environment environment =  new Environment((String) step.getScenarioContext().get("environment"));
            HttpEntity<ExecutionRequestDto> request = new HttpEntity<>(ExecutionRequestMapper.from(step.definition(), dataset, environment), headers);
            StepExecutionReportDto reportDto = restTemplate.postForObject("https://" + delegate.host() + ":" + delegate.port() + EXECUTION_URL, request, StepExecutionReportDto.class);
            return StepExecutionReportMapper.fromDto(reportDto);
        } else {
            throw new CannotDelegateException(delegate);
        }
    }

    private void addBasicAuth(String user, String password) {
        if (ofNullable(user).isPresent()) {
            restTemplate.getInterceptors().add(
                new BasicAuthenticationInterceptor(user, password)
            );
        }
    }
}
