/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package blackbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.enedis.chutney.ServerBootstrap;
import fr.enedis.chutney.campaign.api.CampaignController;
import fr.enedis.chutney.dataset.api.DataSetController;
import fr.enedis.chutney.environment.api.environment.EnvironmentController;
import fr.enedis.chutney.scenario.api.GwtTestCaseController;
import fr.enedis.chutney.security.domain.Authorizations;
import fr.enedis.chutney.server.core.domain.security.Authorization;
import fr.enedis.chutney.server.core.domain.security.Role;
import fr.enedis.chutney.server.core.domain.security.User;
import fr.enedis.chutney.server.core.domain.security.UserRoles;
import fr.enedis.chutney.tokens.domain.AccessTokensService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Endpoints called by the kotlin-dsl synchronization (ChutneyServerServiceImpl) with a personal token.
 */
@SpringBootTest(classes = {ServerBootstrap.class})
@TestPropertySource(properties = "spring.config.additional-location=classpath:blackbox/")
class ApiKeySynchronizationSpringBootIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private Authorizations authorizations;
    @Autowired
    private AccessTokensService accessTokensService;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private MockMvc mvc;
    private String apiKey;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Role role = Role.builder()
            .withName("sync")
            .withAuthorizations(Arrays.stream(Authorization.values()).map(Enum::name).toList())
            .build();
        User user = User.builder().withId("syncUser").withRole("sync").build();
        authorizations.save(UserRoles.builder().withRoles(List.of(role)).withUsers(List.of(user)).build());
        apiKey = accessTokensService.createToken("syncUser", "sync", Instant.now().plus(1, ChronoUnit.DAYS)).token();
    }

    private static Object[][] synchronizationEndpoints() {
        return new Object[][]{
            {GET, GwtTestCaseController.BASE_URL, null},
            {GET, GwtTestCaseController.BASE_URL + "/raw/1", null},
            {POST, GwtTestCaseController.BASE_URL + "/raw", "{\"title\":\"t\",\"content\":\"{\\\"when\\\":{}}\"}"},
            {GET, EnvironmentController.BASE_URL, null},
            {GET, DataSetController.BASE_URL + "/unknownDataset", null},
            {POST, DataSetController.BASE_URL, "{\"name\": \"syncDs\"}"},
            {PUT, DataSetController.BASE_URL, "{\"name\": \"syncDs\"}"},
            {POST, CampaignController.BASE_URL, "{\"title\":\"t\",\"description\":\"d\",\"scenarios\":[],\"tags\":[],\"environment\":\"DEFAULT\",\"parallelRun\":false,\"retryAuto\":false}"},
        };
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("synchronizationEndpoints")
    void synchronization_endpoints_accept_personal_token(HttpMethod method, String url, String content) throws Exception {
        var request = request(method, url)
            .secure(true)
            .header("X-API-KEY", apiKey)
            .contentType(MediaType.APPLICATION_JSON);
        if (content != null) {
            request.content(content);
        }

        int status = mvc.perform(request).andReturn().getResponse().getStatus();

        assertThat(status).as("%s %s", method, url).isNotIn(401, 403);
    }

    @Test
    void scenario_synchronization_creates_then_updates_with_personal_token() throws Exception {
        String rawUrl = GwtTestCaseController.BASE_URL + "/raw";

        String id = perform(POST, rawUrl, """
            {"id": "987654", "title": "created", "content": "{\\"when\\":{}}", "tags": [], "defaultDataset": null}
            """);
        assertThat(id).isEqualTo("987654");

        JsonNode created = objectMapper.readTree(perform(GET, rawUrl + "/" + id, null));
        assertThat(created.get("title").asString()).isEqualTo("created");
        assertThat(created.get("author").asString()).isEqualTo("syncUser");

        perform(POST, rawUrl, """
            {"id": "%s", "title": "updated", "content": "{\\"when\\":{}}", "tags": [], "version": %d, "defaultDataset": null}
            """.formatted(id, created.get("version").asInt()));

        JsonNode updated = objectMapper.readTree(perform(GET, rawUrl + "/" + id, null));
        assertThat(updated.get("title").asString()).isEqualTo("updated");
        assertThat(updated.get("version").asInt()).isGreaterThan(created.get("version").asInt());
    }

    private String perform(HttpMethod method, String url, String content) throws Exception {
        var request = request(method, url)
            .secure(true)
            .header("X-API-KEY", apiKey)
            .contentType(MediaType.APPLICATION_JSON);
        if (content != null) {
            request.content(content);
        }
        return mvc.perform(request)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }
}
