/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package blackbox;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.enedis.chutney.ServerBootstrap;
import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.security.domain.Authorizations;
import fr.enedis.chutney.security.infra.jwt.JwtUtil;
import fr.enedis.chutney.server.core.domain.security.Role;
import fr.enedis.chutney.server.core.domain.security.User;
import fr.enedis.chutney.server.core.domain.security.UserRoles;
import fr.enedis.chutney.tools.file.FileUtils;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = {ServerBootstrap.class})
@TestPropertySource(properties = "spring.config.additional-location=classpath:blackbox/")
public class SecuredControllerSpringBootIntegrationTest {

    @Autowired
    private Authorizations authorizations;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @BeforeAll
    public static void cleanUp() {
        FileUtils.deleteFolder(new File("./target/.chutney").toPath());
    }

    @BeforeEach
    public void setup() {

        mvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    private static Object[] securedEndPointList() {
        return new Object[][]{
            {GET, "/actuator", "ADMIN_ACCESS", null, OK},
            {GET, "/actuator/health", "ADMIN_ACCESS", null, OK},
            {GET, "/actuator/prometheus", "ADMIN_ACCESS", null, NOT_FOUND},

            {GET, "/api/v1/backups", "ADMIN_ACCESS", null, OK},
            {POST, "/api/v1/backups", "ADMIN_ACCESS", "{\"backupables\": [ \"environments\" ]}", OK},
            {GET, "/api/v1/backups/backupId", "ADMIN_ACCESS", null, NOT_FOUND},
            {DELETE, "/api/v1/backups/backupId", "ADMIN_ACCESS", null, NOT_FOUND},
            {GET, "/api/v1/backups/id/download", "ADMIN_ACCESS", null, OK},
            {GET, "/api/v1/backups/backupables", "ADMIN_ACCESS", null, OK},

            {GET, "/api/v1/execution/search?query=abc", "EXECUTION_READ", null, OK},
            {POST, "/api/v1/admin/database/compact", "ADMIN_ACCESS", null, NOT_IMPLEMENTED},
            {GET, "/api/v1/admin/database/size", "ADMIN_ACCESS", null, OK},

            {POST, "/api/v1/agentnetwork/configuration", "ADMIN_ACCESS", "{}", OK},
            {GET, "/api/v1/description", "ADMIN_ACCESS", null, OK},
            {POST, "/api/v1/agentnetwork/explore", "ADMIN_ACCESS", "{\"creationDate\":\"1235\"}", OK},

            {POST, "/api/ui/campaign/v1", "CAMPAIGN_WRITE", "{\"title\":\"secu\",\"description\":\"desc\",\"scenarios\":[],\"tags\":[]}", BAD_REQUEST},
            {POST, "/api/ui/campaign/v1", "CAMPAIGN_WRITE", "{\"title\":\"secu\",\"description\":\"desc\",\"scenarios\":[],\"tags\":[],\"environment\":\"ENV\"}", OK},
            {PUT, "/api/ui/campaign/v1", "CAMPAIGN_WRITE", "{\"title\":\"secu\",\"description\":\"desc\",\"scenarios\":[],\"tags\":[],\"environment\":\"ENV\"}", OK},
            {DELETE, "/api/ui/campaign/v1/123", "CAMPAIGN_WRITE", null, OK},
            {GET, "/api/ui/campaign/v1/123", "CAMPAIGN_READ", null, NOT_FOUND},
            {GET, "/api/ui/campaign/v1/123/scenarios", "CAMPAIGN_READ", null, NOT_FOUND},
            {GET, "/api/ui/campaign/v1", "CAMPAIGN_READ", null, OK},
            {GET, "/api/ui/campaign/v1/lastexecutions/20", "CAMPAIGN_READ", null, OK},
            {GET, "/api/ui/campaign/v1/scenario/scenarioId", "SCENARIO_READ", null, OK},
            {GET, "/api/ui/campaign/v1/scheduling", "CAMPAIGN_READ", null, OK},
            {POST, "/api/ui/campaign/v1/scheduling", "CAMPAIGN_WRITE",
                """
                {"id":1,"schedulingDate":[2024,10,12,14,30,45],"frequency":"Daily","environment":"PROD","campaignsId":[1],"campaignsTitle":["title"],"datasetsId":["datasetId"]}
                """, OK},
            {DELETE, "/api/ui/campaign/v1/scheduling/123", "CAMPAIGN_WRITE", null, OK},

            {GET, "/api/ui/campaign/execution/v1/campaignName", "EXECUTION_WRITE", null, OK},
            {GET, "/api/ui/campaign/execution/v1/campaignName/DEFAULT", "EXECUTION_WRITE", null, OK},
            {POST, "/api/ui/campaign/execution/v1/replay/123", "EXECUTION_WRITE", "{}", NOT_FOUND},
            {GET, "/api/ui/campaign/execution/v1/campaignPattern/surefire", "EXECUTION_WRITE", null, OK},
            {GET, "/api/ui/campaign/execution/v1/campaignPattern/surefire/DEFAULT", "EXECUTION_WRITE", null, OK},
            {POST, "/api/ui/campaign/execution/v1/123/stop", "EXECUTION_WRITE", "{}", NOT_FOUND},
            {POST, "/api/ui/campaign/execution/v1/byID/123", "EXECUTION_WRITE", "{}", NOT_FOUND},
            {POST, "/api/ui/campaign/execution/v1/byID/123/DEFAULT", "EXECUTION_WRITE", "{}", NOT_FOUND},
            {GET, "/api/ui/campaign/v1/execution/1", "CAMPAIGN_READ", null, NOT_FOUND},

            {GET, "/api/v1/editions/testcases/testcaseId", "SCENARIO_READ", null, OK},
            {POST, "/api/v1/editions/testcases/testcaseId", "SCENARIO_WRITE", "{}", NOT_FOUND},
            {DELETE, "/api/v1/editions/testcases/testcaseId", "SCENARIO_WRITE", null, OK},
            {GET, "/api/ui/jira/v1/scenario", "SCENARIO_READ", null, OK},
            {GET, "/api/ui/jira/v1/scenario", "CAMPAIGN_WRITE", null, OK},
            {GET, "/api/ui/jira/v1/campaign", "CAMPAIGN_READ", null, OK},
            {GET, "/api/ui/jira/v1/scenario/scenarioId", "SCENARIO_READ", null, OK},
            {POST, "/api/ui/jira/v1/scenario", "SCENARIO_WRITE", "{\"id\":\"\",\"chutneyId\":\"\"}", OK},
            {DELETE, "/api/ui/jira/v1/scenario/scenarioId", "SCENARIO_WRITE", null, OK},
            {GET, "/api/ui/jira/v1/campaign/campaignId", "CAMPAIGN_READ", null, OK},
            // {GET, "/api/ui/jira/v1/testexec/testExecId", "CAMPAIGN_WRITE", null, OK}, need a valid jira url
            {PUT, "/api/ui/jira/v1/testexec/testExecId", "CAMPAIGN_WRITE", "{\"id\":\"\",\"chutneyId\":\"\"}", OK},
            {POST, "/api/ui/jira/v1/campaign", "CAMPAIGN_WRITE", "{\"id\":\"\",\"chutneyId\":\"\"}", OK},
            {DELETE, "/api/ui/jira/v1/campaign/campaignId", "CAMPAIGN_WRITE", null, OK},
            {GET, "/api/ui/jira/v1/configuration", "ADMIN_ACCESS", null, OK},
            {GET, "/api/ui/jira/v1/configuration/url", "SCENARIO_READ", null, OK},
            {GET, "/api/ui/jira/v1/configuration/url", "CAMPAIGN_READ", null, OK},
            {POST, "/api/ui/jira/v1/configuration", "ADMIN_ACCESS", "{\"url\":\"\",\"username\":\"\",\"password\":\"\",\"urlProxy\":\"\",\"userProxy\":\"\",\"passwordProxy\":\"\"}", OK},

            {POST, "/api/v1/ui/plugins/linkifier/", "ADMIN_ACCESS", "{\"pattern\":\"\",\"link\":\"\",\"id\":\"\"}", OK},
            {DELETE, "/api/v1/ui/plugins/linkifier/id", "ADMIN_ACCESS", null, OK},

            {GET, "/api/scenario/v2/1", "SCENARIO_READ", null, NOT_FOUND},
            {GET, "/api/scenario/v2/testCaseId/metadata", "SCENARIO_READ", null, NOT_FOUND},
            {GET, "/api/scenario/v2", "SCENARIO_READ", null, OK},
            {GET, "/api/scenario/v2", "CAMPAIGN_READ", null, OK},
            {POST, "/api/scenario/v2", "SCENARIO_WRITE", "{\"title\":\"\",\"scenario\":{\"when\":{}}}", OK},
            {PATCH, "/api/scenario/v2", "SCENARIO_WRITE", "{\"title\":\"\",\"scenario\":{\"when\":{}}}", OK},
            {DELETE, "/api/scenario/v2/testCaseId", "SCENARIO_WRITE", null, OK},
            {POST, "/api/scenario/v2/raw", "SCENARIO_WRITE", "{\"title\":\"\",\"content\":\"{\\\"when\\\":{}}\"}", OK},
            {GET, "/api/scenario/v2/raw/1", "SCENARIO_READ", null, OK},

            {POST, "/api/scenario/execution/v1", "EXECUTION_WRITE", "{\"scenario\":{},\"environment\": {\"name\":\"env\"}}", OK},

            {GET, "/api/ui/scenario/123/execution/v1", "EXECUTION_READ", null, OK},
            {GET, "/api/ui/scenario/123/execution/123/v1", "EXECUTION_READ", null, NOT_FOUND},
            {GET, "/api/ui/scenario/execution/123/summary/v1", "EXECUTION_READ", null, NOT_FOUND},
            {POST, "/api/ui/scenario/execution/v1/scenarioId/secuenv", "EXECUTION_WRITE", null, NOT_FOUND},
            {POST, "/api/idea/scenario/execution/DEFAULT", "EXECUTION_WRITE", "{\"content\":\"{\\\"when\\\":{}}\",\"params\":{}} ", OK},
            {POST, "/api/ui/scenario/executionasync/v1/scenarioId/DEFAULT", "EXECUTION_WRITE", null, NOT_FOUND},
            {POST, "/api/ui/scenario/executionasync/v1/scenarioId/DEFAULT/DATASET", "EXECUTION_WRITE", "[]", NOT_FOUND},
            {GET, "/api/ui/scenario/executionasync/v1/scenarioId/execution/123", "EXECUTION_READ", null, NOT_FOUND},
            {POST, "/api/ui/scenario/executionasync/v1/scenarioId/execution/123/stop", "EXECUTION_WRITE", null, NOT_FOUND},
            {POST, "/api/ui/scenario/executionasync/v1/scenarioId/execution/123/pause", "EXECUTION_WRITE", null, NOT_FOUND},
            {POST, "/api/ui/scenario/executionasync/v1/scenarioId/execution/123/resume", "EXECUTION_WRITE", null, NOT_FOUND},

            {POST, "/api/v1/authorizations", "ADMIN_ACCESS", "{}", OK},
            {GET, "/api/v1/authorizations", "ADMIN_ACCESS", null, OK},

            {GET, "/api/action/v1", "SCENARIO_READ", null, OK},
            {GET, "/api/action/v1/actionId", "SCENARIO_READ", null, NOT_FOUND},

            // Environments module
            {GET, "/api/v2/environments", "ENVIRONMENT_READ", null, OK},
            {GET, "/api/v2/environments/names", "CAMPAIGN_READ", null, OK},
            {GET, "/api/v2/environments/names", "TARGET_WRITE", null, OK},
            {GET, "/api/v2/environments/names", "EXECUTION_WRITE", null, OK},
            {GET, "/api/v2/environments/names", "ENVIRONMENT_READ", null, OK},
            {POST, "/api/v2/environments", "ENVIRONMENT_WRITE", "{\"name\": \"secuenv\"} ", OK},
            {DELETE, "/api/v2/environments/envName", "ENVIRONMENT_WRITE", null, NOT_FOUND},
            {PUT, "/api/v2/environments/envName", "ENVIRONMENT_WRITE", "{}", NOT_FOUND},
            {GET, "/api/v2/environments/envName/target", "ENVIRONMENT_READ", null, NOT_FOUND},
            {GET, "/api/v2/environments/envName", "ENVIRONMENT_READ", null, NOT_FOUND},

            {GET, "/api/v2/environments/targets", "TARGET_READ", null, OK},
            {GET, "/api/v2/environments/envName/targets/targetName", "TARGET_READ", null, NOT_FOUND},
            {DELETE, "/api/v2/environments/envName/targets/targetName", "TARGET_WRITE", null, NOT_FOUND},
            {POST, "/api/v2/targets", "TARGET_WRITE", "{\"name\":\"targetName\",\"url\":\"http://localhost\", \"environment\":\"secuenv\"}", OK},
            {PUT, "/api/v2/targets/targetName", "TARGET_WRITE", "{\"name\":\"targetName\",\"url\":\"https://localhost\", \"environment\":\"secuenv\"}", OK},

            {GET, "/api/v2/environments/variables", "VARIABLE_READ", null, OK},
            {POST, "/api/v2/variables", "VARIABLE_WRITE", "[]", OK},
            {PUT, "/api/v2/variables/varKey", "VARIABLE_WRITE", "[]", OK},
            {DELETE, "/api/v2/variables/varKey", "VARIABLE_WRITE", null, NOT_FOUND},

            // Must be at the end because the network configuration is in wrong state, why ??
            {POST, "/api/v1/agentnetwork/wrapup", "ADMIN_ACCESS", "{\"agentsGraph\":{\"agents\":[]},\"networkConfiguration\":{\"creationDate\":\"2021-09-06T10:08:36.569227Z\",\"agentNetworkConfiguration\":[],\"environmentsConfiguration\":[]}}", OK},
        };
    }

    private static Object[] unsecuredEndPointList() {
        return new Object[][]{
            {GET, "/api/v1/user", "AUTHENTICATED", null, OK},
            {POST, "/api/v1/user", "AUTHENTICATED", "{}", OK},
            {GET, "/api/v1/ui/plugins/linkifier/", "AUTHENTICATED", null, OK},
            {GET, "/api/v1/info/build/version", null, null, OK},
            {GET, "/api/v1/info/appname", null, null, OK},
            {GET, "/api/v2/features", "AUTHENTICATED", null, OK},
            {GET, "/api/v1/sso/config", null, null, OK},
        };
    }

    @ParameterizedTest
    @MethodSource({"securedEndPointList", "unsecuredEndPointList"})
    public void secured_api_access_verification(HttpMethod httpMethod, String url, String authority, String content, HttpStatus status) throws Exception {
        UserDto userDto = new UserDto();
        userDto.setName("admin");
        userDto.setId("admin");
        MockHttpServletRequestBuilder request = request(httpMethod, url)
            .secure(true)
            .contentType(MediaType.APPLICATION_JSON);
        if (content != null) {
            request.content(content);
        }
        manageAuth(authority, userDto, request);

        mvc.perform(request)
            .andExpect(status().is(status.value()));
    }

    @ParameterizedTest
    @MethodSource({"uploadEndpoints"})
    public void secured_upload_api_access_verification(String url, String authority, String content, HttpStatus expectedStatus) throws Exception {
        UserDto userDto = new UserDto();
        userDto.setName("admin");
        userDto.setId("admin");
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .multipart(url)
            .file(new MockMultipartFile("file", "myFile.json", "text/json", content.getBytes()))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .secure(true);
        manageAuth(authority, userDto, request);

        mvc.perform(request)
            .andExpect(status().is(expectedStatus.value()));
    }

    private static Stream<Arguments> uploadEndpoints() {
        return Stream.of(
            Arguments.of("/api/v2/environments", "ENVIRONMENT_WRITE", "{\"name\": \"env\"}", OK),
            Arguments.of("/api/v2/environments/env/targets", "TARGET_WRITE", "{\"name\":\"targetName\",\"url\":\"tcp://localhost\", \"environment\":\"env\"}", OK)
        );
    }

    private void manageAuth(String authority, UserDto userDto, MockHttpServletRequestBuilder request) {
        ofNullable(authority).ifPresent(right -> {
            if (!"AUTHENTICATED".equals(authority)) {
                Role adminRole = Role.builder().withAuthorizations(List.of(right)).withName("admin").build();
                User user = User.builder().withRole("admin").withId("admin").build();
                authorizations.save(UserRoles.builder().withRoles(List.of(adminRole)).withUsers(List.of(user)).build());
                userDto.grantAuthority(right);
            }
            String token = jwtUtil.generateToken(userDto.getId(), objectMapper.convertValue(userDto, Map.class));
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
        });
    }
}
