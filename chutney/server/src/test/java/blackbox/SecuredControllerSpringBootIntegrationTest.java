/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package blackbox;

import static fr.enedis.chutney.server.core.domain.security.Authorization.ADMIN_ACCESS;
import static fr.enedis.chutney.server.core.domain.security.Authorization.CAMPAIGN_READ;
import static fr.enedis.chutney.server.core.domain.security.Authorization.CAMPAIGN_WRITE;
import static fr.enedis.chutney.server.core.domain.security.Authorization.ENVIRONMENT_READ;
import static fr.enedis.chutney.server.core.domain.security.Authorization.ENVIRONMENT_WRITE;
import static fr.enedis.chutney.server.core.domain.security.Authorization.EXECUTION_READ;
import static fr.enedis.chutney.server.core.domain.security.Authorization.EXECUTION_WRITE;
import static fr.enedis.chutney.server.core.domain.security.Authorization.SCENARIO_READ;
import static fr.enedis.chutney.server.core.domain.security.Authorization.SCENARIO_WRITE;
import static fr.enedis.chutney.server.core.domain.security.Authorization.TARGET_READ;
import static fr.enedis.chutney.server.core.domain.security.Authorization.TARGET_WRITE;
import static fr.enedis.chutney.server.core.domain.security.Authorization.VARIABLE_READ;
import static fr.enedis.chutney.server.core.domain.security.Authorization.VARIABLE_WRITE;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.enedis.chutney.ServerBootstrap;
import fr.enedis.chutney.action.api.ActionController;
import fr.enedis.chutney.admin.api.BackupController;
import fr.enedis.chutney.admin.api.DatabaseManagementController;
import fr.enedis.chutney.admin.api.InfoController;
import fr.enedis.chutney.agent.api.NodeNetworkController;
import fr.enedis.chutney.campaign.api.CampaignController;
import fr.enedis.chutney.campaign.api.ScheduleCampaignController;
import fr.enedis.chutney.dataset.api.DataSetController;
import fr.enedis.chutney.design.api.editionlock.TestCaseEditionController;
import fr.enedis.chutney.design.api.plugins.linkifier.LinkifierController;
import fr.enedis.chutney.engine.api.execution.HttpTestEngine;
import fr.enedis.chutney.environment.api.environment.EnvironmentController;
import fr.enedis.chutney.environment.api.target.TargetController;
import fr.enedis.chutney.environment.api.variable.EnvironmentVariableController;
import fr.enedis.chutney.execution.api.CampaignExecutionUiController;
import fr.enedis.chutney.execution.api.ScenarioExecutionHistoryController;
import fr.enedis.chutney.execution.api.ScenarioExecutionUiController;
import fr.enedis.chutney.execution.api.report.search.ExecutionSearchController;
import fr.enedis.chutney.feature.api.FeatureController;
import fr.enedis.chutney.index.api.SearchController;
import fr.enedis.chutney.jira.api.JiraController;
import fr.enedis.chutney.scenario.api.AggregatedTestCaseController;
import fr.enedis.chutney.scenario.api.GwtTestCaseController;
import fr.enedis.chutney.security.api.AuthorizationController;
import fr.enedis.chutney.security.api.SsoOpenIdConnectController;
import fr.enedis.chutney.security.api.UserController;
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
            // Actuator
            {GET, "/actuator", ADMIN_ACCESS.name(), null, OK},
            {GET, "/actuator/health", ADMIN_ACCESS.name(), null, OK},
            {GET, "/actuator/prometheus", ADMIN_ACCESS.name(), null, NOT_FOUND},

            // Server
            {GET, BackupController.BASE_URL, ADMIN_ACCESS.name(), null, OK},
            {POST, BackupController.BASE_URL, ADMIN_ACCESS.name(), "{\"backupables\": [ \"environments\" ]}", OK},
            {GET, BackupController.BASE_URL + "/backupId", ADMIN_ACCESS.name(), null, NOT_FOUND},
            {DELETE, BackupController.BASE_URL + "/backupId", ADMIN_ACCESS.name(), null, NOT_FOUND},
            {GET, BackupController.BASE_URL + "/id/download", ADMIN_ACCESS.name(), null, OK},
            {GET, BackupController.BASE_URL + "/backupables", ADMIN_ACCESS.name(), null, OK},

            {POST, DatabaseManagementController.BASE_URL + "/compact", ADMIN_ACCESS.name(), null, NOT_IMPLEMENTED},
            {GET, DatabaseManagementController.BASE_URL + "/size", ADMIN_ACCESS.name(), null, OK},

            {POST, NodeNetworkController.CONFIGURE_URL, ADMIN_ACCESS.name(), "{}", OK},
            {GET, NodeNetworkController.DESCRIPTION_URL, ADMIN_ACCESS.name(), null, OK},
            {POST, NodeNetworkController.EXPLORE_URL, ADMIN_ACCESS.name(), "{\"creationDate\":\"1235\"}", OK},

            {POST, CampaignController.BASE_URL, CAMPAIGN_WRITE.name(), "{\"title\":\"secu\",\"description\":\"desc\",\"scenarios\":[],\"tags\":[]}", BAD_REQUEST},
            {POST, CampaignController.BASE_URL, CAMPAIGN_WRITE.name(), "{\"title\":\"secu\",\"description\":\"desc\",\"scenarios\":[],\"tags\":[],\"environment\":\"DEFAULT\"}", OK},
            {PUT, CampaignController.BASE_URL, CAMPAIGN_WRITE.name(), "{\"title\":\"secu\",\"description\":\"desc\",\"scenarios\":[],\"tags\":[],\"environment\":\"DEFAULT\"}", OK},
            {DELETE, CampaignController.BASE_URL + "/123", CAMPAIGN_WRITE.name(), null, OK},
            {GET, CampaignController.BASE_URL + "/123", CAMPAIGN_READ.name(), null, NOT_FOUND},
            {GET, CampaignController.BASE_URL + "/execution/1", CAMPAIGN_READ.name(), null, NOT_FOUND},
            {GET, CampaignController.BASE_URL + "/123/scenarios", CAMPAIGN_READ.name(), null, NOT_FOUND},
            {GET, CampaignController.BASE_URL, CAMPAIGN_READ.name(), null, OK},
            {GET, CampaignController.BASE_URL, EXECUTION_READ.name(), null, OK},
            {GET, CampaignController.BASE_URL + "/lastexecutions/20", CAMPAIGN_READ.name(), null, OK},
            {GET, CampaignController.BASE_URL + "/lastexecutions/20", EXECUTION_READ.name(), null, OK},
            {GET, CampaignController.BASE_URL + "/scenario/scenarioId", SCENARIO_READ.name(), null, OK},
            {GET, CampaignController.BASE_URL + "/scenario/scenarioId", CAMPAIGN_READ.name(), null, OK},
            {GET, CampaignController.BASE_URL + "/scenario/scenarioId", EXECUTION_READ.name(), null, OK},
            {GET, ScheduleCampaignController.BASE_URL, CAMPAIGN_READ.name(), null, OK},
            {GET, ScheduleCampaignController.BASE_URL, EXECUTION_READ.name(), null, OK},
            {POST, ScheduleCampaignController.BASE_URL, CAMPAIGN_WRITE.name(),
                """
                {"id":1,"schedulingDate":[2024,10,12,14,30,45],"frequency":"Daily","environment":"DEFAULT","campaignsId":[1],"campaignsTitle":["title"],"datasetsId":["datasetId"]}
                """, OK},
            {DELETE, ScheduleCampaignController.BASE_URL + "/123", CAMPAIGN_WRITE.name(), null, OK},

            {GET, DataSetController.BASE_URL, "DATASET_READ", null, OK},
            {GET, DataSetController.BASE_URL, SCENARIO_READ.name(), null, OK},
            {GET, DataSetController.BASE_URL, CAMPAIGN_READ.name(), null, OK},
            {GET, DataSetController.BASE_URL, EXECUTION_WRITE.name(), null, OK},
            {POST, DataSetController.BASE_URL, "DATASET_WRITE", "{\"name\": \"ds\"}", OK},
            {PUT, DataSetController.BASE_URL, "DATASET_WRITE", "{\"name\": \"dss\"}", OK},
            {DELETE, DataSetController.BASE_URL + "/datasetName", "DATASET_WRITE", null, OK},
            {GET, DataSetController.BASE_URL + "/datasetId", "DATASET_WRITE", null, NOT_FOUND},

            {GET, TestCaseEditionController.BASE_URL + "/testcaseId", SCENARIO_READ.name(), null, OK},
            {POST, TestCaseEditionController.BASE_URL + "/testcaseId", SCENARIO_WRITE.name(), "{}", NOT_FOUND},
            {DELETE, TestCaseEditionController.BASE_URL + "/testcaseId", SCENARIO_WRITE.name(), null, OK},

            {POST, LinkifierController.BASE_URL, ADMIN_ACCESS.name(), "{\"pattern\":\"\",\"link\":\"\",\"id\":\"\"}", OK},
            {DELETE, LinkifierController.BASE_URL + "/id", ADMIN_ACCESS.name(), null, OK},

            {GET, CampaignExecutionUiController.BASE_URL + "/123/lastExecution", EXECUTION_READ.name(), null, NOT_FOUND},
            {GET, CampaignExecutionUiController.BASE_URL + "/123/DEFAULT/lastExecution", EXECUTION_READ.name(), null, NOT_FOUND},
            {GET, CampaignExecutionUiController.BASE_URL + "/campaignName", EXECUTION_WRITE.name(), null, OK},
            {GET, CampaignExecutionUiController.BASE_URL + "/campaignName/DEFAULT", EXECUTION_WRITE.name(), null, OK},
            {POST, CampaignExecutionUiController.BASE_URL + "/replay/123", EXECUTION_WRITE.name(), "{}", NOT_FOUND},
            {GET, CampaignExecutionUiController.BASE_URL + "/campaignPattern/surefire", EXECUTION_WRITE.name(), null, OK},
            {GET, CampaignExecutionUiController.BASE_URL + "/campaignPattern/surefire/DEFAULT", EXECUTION_WRITE.name(), null, OK},
            {POST, CampaignExecutionUiController.BASE_URL + "/123/stop", EXECUTION_WRITE.name(), "{}", NOT_FOUND},
            {POST, CampaignExecutionUiController.BASE_URL + "/byID/123", EXECUTION_WRITE.name(), "{}", NOT_FOUND},
            {POST, CampaignExecutionUiController.BASE_URL + "/byID/123/DEFAULT", EXECUTION_WRITE.name(), "{}", NOT_FOUND},

            {GET, ScenarioExecutionHistoryController.BASE_URL + "/123/execution/v1", EXECUTION_READ.name(), null, OK},
            {GET, ScenarioExecutionHistoryController.BASE_URL + "/123/summary/v1", EXECUTION_READ.name(), null, NOT_FOUND},
            {GET, ScenarioExecutionHistoryController.BASE_URL + "/123/execution/123/v1", EXECUTION_READ.name(), null, NOT_FOUND},
            {DELETE, ScenarioExecutionHistoryController.BASE_URL + "/execution/123", EXECUTION_WRITE.name(), null, NOT_FOUND},

            {POST, ScenarioExecutionUiController.IDEA_BASE_URL + "/DEFAULT", EXECUTION_WRITE.name(), "{\"content\":\"{\\\"when\\\":{}}\",\"params\":{}} ", OK},
            {POST, ScenarioExecutionUiController.BASE_URL + "/scenarioId/DEFAULT", EXECUTION_WRITE.name(), null, NOT_FOUND},
            {POST, ScenarioExecutionUiController.BASE_URL + "/scenarioId", EXECUTION_WRITE.name(), null, NOT_FOUND},
            {POST, ScenarioExecutionUiController.SYNC_BASE_URL + "/scenarioId/DEFAULT", EXECUTION_WRITE.name(), null, NOT_FOUND},
            {GET, ScenarioExecutionUiController.BASE_URL + "/scenarioId/execution/123", EXECUTION_READ.name(), null, NOT_FOUND},
            {POST, ScenarioExecutionUiController.BASE_URL + "/scenarioId/execution/123/stop", EXECUTION_WRITE.name(), null, NOT_FOUND},
            {POST, ScenarioExecutionUiController.BASE_URL + "/scenarioId/execution/123/pause", EXECUTION_WRITE.name(), null, NOT_FOUND},
            {POST, ScenarioExecutionUiController.BASE_URL + "/scenarioId/execution/123/resume", EXECUTION_WRITE.name(), null, NOT_FOUND},

            {GET, ExecutionSearchController.BASE_URL + "/search?query=abc", EXECUTION_READ.name(), null, OK},

            {GET, SearchController.BASE_URL + "?keyword=abc", SCENARIO_READ.name(), null, OK},
            {GET, SearchController.BASE_URL + "?keyword=abc", CAMPAIGN_READ.name(), null, OK},
            {GET, SearchController.BASE_URL + "?keyword=abc", "DATASET_READ", null, OK},

            {GET, AggregatedTestCaseController.BASE_URL + "/testCaseId/metadata", SCENARIO_READ.name(), null, NOT_FOUND},
            {GET, AggregatedTestCaseController.BASE_URL + "/testCaseId/metadata", EXECUTION_READ.name(), null, NOT_FOUND},
            {GET, AggregatedTestCaseController.BASE_URL, SCENARIO_READ.name(), null, OK},
            {GET, AggregatedTestCaseController.BASE_URL, CAMPAIGN_READ.name(), null, OK},
            {GET, AggregatedTestCaseController.BASE_URL, EXECUTION_READ.name(), null, OK},
            {DELETE, AggregatedTestCaseController.BASE_URL + "/testCaseId", SCENARIO_WRITE.name(), null, OK},

            {GET, GwtTestCaseController.BASE_URL + "/1", SCENARIO_READ.name(), null, NOT_FOUND},
            {GET, GwtTestCaseController.BASE_URL + "/1", EXECUTION_READ.name(), null, NOT_FOUND},
            {POST, GwtTestCaseController.BASE_URL, SCENARIO_WRITE.name(), "{\"title\":\"\",\"scenario\":{\"when\":{}}}", OK},
            {PATCH, GwtTestCaseController.BASE_URL, SCENARIO_WRITE.name(), "{\"title\":\"\",\"scenario\":{\"when\":{}}}", OK},
            {POST, GwtTestCaseController.BASE_URL + "/raw", SCENARIO_WRITE.name(), "{\"title\":\"\",\"content\":\"{\\\"when\\\":{}}\"}", OK},
            {GET, GwtTestCaseController.BASE_URL + "/raw/1", SCENARIO_READ.name(), null, OK},

            {POST, AuthorizationController.BASE_URL, ADMIN_ACCESS.name(), "{}", OK},
            {GET, AuthorizationController.BASE_URL, ADMIN_ACCESS.name(), null, OK},

            // Engine
            {GET, ActionController.BASE_URL, SCENARIO_READ.name(), null, OK},
            {GET, ActionController.BASE_URL + "/actionId", SCENARIO_READ.name(), null, NOT_FOUND},
            {POST, HttpTestEngine.EXECUTION_URL, EXECUTION_WRITE.name(), "{\"scenario\":{},\"environment\": {\"name\":\"DEFAULT\"}}", OK},

            // Environment (order is important)
            {GET, EnvironmentController.BASE_URL, ENVIRONMENT_READ.name(), null, OK},
            {GET, EnvironmentController.BASE_URL, ADMIN_ACCESS.name(), null, OK},
            {GET, EnvironmentController.BASE_URL + "/names", ENVIRONMENT_READ.name(), null, OK},
            {GET, EnvironmentController.BASE_URL + "/names", CAMPAIGN_READ.name(), null, OK},
            {GET, EnvironmentController.BASE_URL + "/names", TARGET_WRITE.name(), null, OK},
            {GET, EnvironmentController.BASE_URL + "/names", EXECUTION_WRITE.name(), null, OK},
            {POST, EnvironmentController.BASE_URL, ENVIRONMENT_WRITE.name(), "{\"name\": \"newEnv\"}", OK},
            {PUT, EnvironmentController.BASE_URL + "/newEnv", ENVIRONMENT_WRITE.name(), "{\"name\": \"newEnv\"}", OK},
            {GET, EnvironmentController.BASE_URL + "/newEnv", ENVIRONMENT_READ.name(), null, OK},
            {DELETE, EnvironmentController.BASE_URL + "/newEnv", ENVIRONMENT_WRITE.name(), null, OK}, // keep this at the end to clean

            {GET, EnvironmentController.BASE_URL + "/targets", TARGET_READ.name(), null, OK},
            {GET, EnvironmentController.BASE_URL + "/targets", ADMIN_ACCESS.name(), null, OK},
            {GET, TargetController.TARGET_BASE_URI + "/names", TARGET_READ.name(), null, OK},
            {GET, TargetController.TARGET_BASE_URI, TARGET_READ.name(), null, OK},
            {POST, TargetController.TARGET_BASE_URI, TARGET_WRITE.name(), "{\"name\":\"targetName\",\"url\":\"http://localhost\", \"environment\":\"DEFAULT\"}", OK},
            {GET, EnvironmentController.BASE_URL + "/DEFAULT/targets/targetName", TARGET_READ.name(), null, OK},
            {PUT, TargetController.TARGET_BASE_URI + "/targetName", TARGET_WRITE.name(), "{\"name\":\"targetName\",\"url\":\"https://localhost\", \"environment\":\"DEFAULT\"}", OK},
            {DELETE, EnvironmentController.BASE_URL + "/DEFAULT/targets/targetName", TARGET_WRITE.name(), null, OK},
            {DELETE, TargetController.TARGET_BASE_URI + "/targetName", TARGET_WRITE.name(), null, OK},

            {GET, EnvironmentController.BASE_URL + "/variables", VARIABLE_READ.name(), null, OK},
            {POST, EnvironmentVariableController.VARIABLE_BASE_URI, VARIABLE_WRITE.name(), "[{\"key\":\"varKey\", \"value\":\"varValue\", \"env\":\"DEFAULT\"}]", OK},
            {PUT, EnvironmentVariableController.VARIABLE_BASE_URI + "/varKey", VARIABLE_WRITE.name(), "[{\"key\":\"varKey\", \"value\":\"varValue\", \"env\":\"DEFAULT\"}]", OK},
            {DELETE, EnvironmentVariableController.VARIABLE_BASE_URI + "/varKey", VARIABLE_WRITE.name(), null, OK},

            // Jira
            {GET, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL, SCENARIO_READ.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL, CAMPAIGN_WRITE.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL, EXECUTION_READ.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_CAMPAIGN_URL, CAMPAIGN_READ.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_CAMPAIGN_URL, EXECUTION_READ.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL + "/scenarioId", SCENARIO_READ.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL + "/scenarioId", CAMPAIGN_WRITE.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL + "/scenarioId", EXECUTION_READ.name(), null, OK},

            {POST, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL, SCENARIO_WRITE.name(), "{\"id\":\"\",\"chutneyId\":\"\"}", OK},
            {POST, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL, CAMPAIGN_WRITE.name(), "{\"id\":\"\",\"chutneyId\":\"\"}", OK},
            {DELETE, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL + "/scenarioId", SCENARIO_WRITE.name(), null, OK},
            {DELETE, JiraController.BASE_URL + JiraController.BASE_SCENARIO_URL + "/scenarioId", CAMPAIGN_WRITE.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_CAMPAIGN_URL + "/campaignId", CAMPAIGN_READ.name(), null, OK},
            // {GET, "/api/ui/jira/v1/testexec/testExecId", Authorization.CAMPAIGN_WRITE.name(), null, OK}, need a valid jira url
            {GET, JiraController.BASE_URL + JiraController.BASE_CAMPAIGN_EXEC_URL + "/campaignExecutionId", CAMPAIGN_WRITE.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_CAMPAIGN_EXEC_URL + "/campaignExecutionId", EXECUTION_READ.name(), null, OK},
            {POST, JiraController.BASE_URL + JiraController.BASE_CAMPAIGN_URL, CAMPAIGN_WRITE.name(), "{\"id\":\"\",\"chutneyId\":\"\"}", OK},
            {DELETE, JiraController.BASE_URL + JiraController.BASE_CAMPAIGN_URL + "/campaignId", CAMPAIGN_WRITE.name(), null, OK},

            {GET, JiraController.BASE_URL + JiraController.BASE_CONFIGURATION_URL, ADMIN_ACCESS.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_CONFIGURATION_URL + "/url", SCENARIO_READ.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_CONFIGURATION_URL + "/url", CAMPAIGN_READ.name(), null, OK},
            {GET, JiraController.BASE_URL + JiraController.BASE_CONFIGURATION_URL + "/url", EXECUTION_READ.name(), null, OK},
            {POST, JiraController.BASE_URL + JiraController.BASE_CONFIGURATION_URL, ADMIN_ACCESS.name(), "{\"url\":\"\",\"username\":\"\",\"password\":\"\",\"urlProxy\":\"\",\"userProxy\":\"\",\"passwordProxy\":\"\"}", OK},
            {DELETE, JiraController.BASE_URL + JiraController.BASE_CONFIGURATION_URL, ADMIN_ACCESS.name(), null, NO_CONTENT},
            {PUT, JiraController.BASE_URL + JiraController.BASE_TEST_EXEC_URL + "/testExecId", CAMPAIGN_WRITE.name(), "{\"id\":\"\",\"chutneyId\":\"\"}", OK},

            // Must be at the end because the network configuration is in wrong state, why ??
            {POST, NodeNetworkController.WRAP_UP_URL, ADMIN_ACCESS.name(), "{\"agentsGraph\":{\"agents\":[]},\"networkConfiguration\":{\"creationDate\":\"2021-09-06T10:08:36.569227Z\",\"agentNetworkConfiguration\":[],\"environmentsConfiguration\":[]}}", OK},
        };
    }

    private static Object[] unsecuredEndPointList() {
        return new Object[][]{
            {GET, UserController.BASE_URL, "AUTHENTICATED", null, OK},
            {POST, UserController.BASE_URL, "AUTHENTICATED", "{}", OK},
            {GET, LinkifierController.BASE_URL, "AUTHENTICATED", null, OK},

            {GET, InfoController.BASE_URL + "/build/version", null, null, OK},
            {GET, InfoController.BASE_URL + "/appname", null, null, OK},

            {GET, FeatureController.BASE_URL, "AUTHENTICATED", null, OK},
            {GET, SsoOpenIdConnectController.BASE_URL + "/config", null, null, OK},
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
            Arguments.of(EnvironmentController.BASE_URL, ENVIRONMENT_WRITE.name(), "{\"name\": \"DEFAULT\"}", CONFLICT),
            Arguments.of(EnvironmentController.BASE_URL + "/envName/targets", TARGET_WRITE.name(), "{\"name\":\"targetName\",\"url\":\"tcp://localhost\", \"environment\":\"unknownEnv\"}", NOT_FOUND)
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
