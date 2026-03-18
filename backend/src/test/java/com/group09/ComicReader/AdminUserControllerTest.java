package com.group09.ComicReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        String loginPayload = """
                {
                  "email": "admin@comicreader.dev",
                  "password": "admin123"
                }
                """;

        String loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        adminToken = objectMapper.readTree(loginResult).get("accessToken").asText();
    }

    @Test
    void adminShouldListUsers() throws Exception {
        String result = mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(result);
        assertThat(json.get("content").isArray()).isTrue();
        assertThat(json.get("content").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void adminShouldBanAndUnbanUser() throws Exception {
        // Get user list to find the demo user ID
        String listResult = mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode users = objectMapper.readTree(listResult).get("content");
        Long demoUserId = null;
        for (JsonNode user : users) {
            if ("user@comicreader.dev".equals(user.get("email").asText())) {
                demoUserId = user.get("id").asLong();
                break;
            }
        }
        assertThat(demoUserId).isNotNull();

        // Ban user
        String banResult = mockMvc.perform(put("/api/v1/admin/users/" + demoUserId + "/ban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode banJson = objectMapper.readTree(banResult);
        assertThat(banJson.get("enabled").asBoolean()).isFalse();

        // Banned user login should fail
        String userLoginPayload = """
                {
                  "email": "user@comicreader.dev",
                  "password": "user123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userLoginPayload))
                .andExpect(status().isBadRequest());

        // Unban user
        String unbanResult = mockMvc.perform(put("/api/v1/admin/users/" + demoUserId + "/unban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode unbanJson = objectMapper.readTree(unbanResult);
        assertThat(unbanJson.get("enabled").asBoolean()).isTrue();

        // After unban, user should be able to login again
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userLoginPayload))
                .andExpect(status().isOk());
    }

    @Test
    void bannedUserTokenShouldBeRejected() throws Exception {
        // Login as user first to get token
        String userLoginPayload = """
                {
                  "email": "user@comicreader.dev",
                  "password": "user123"
                }
                """;

        String userLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userLoginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userToken = objectMapper.readTree(userLoginResult).get("accessToken").asText();

        // Find user ID and ban
        String listResult = mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode users = objectMapper.readTree(listResult).get("content");
        Long userId = null;
        for (JsonNode user : users) {
            if ("user@comicreader.dev".equals(user.get("email").asText())) {
                userId = user.get("id").asLong();
                break;
            }
        }

        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/ban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Banned user's existing token should be rejected with 403
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Cleanup: unban
        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/unban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotAccessAdminEndpoints() throws Exception {
        String userLoginPayload = """
                {
                  "email": "user@comicreader.dev",
                  "password": "user123"
                }
                """;

        String loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userLoginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userToken = objectMapper.readTree(loginResult).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
