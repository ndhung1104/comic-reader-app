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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTopUpPackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // Login as admin
        String adminLoginPayload = """
                {
                  "email": "admin@comicreader.dev",
                  "password": "admin123"
                }
                """;

        String adminResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        adminToken = objectMapper.readTree(adminResult).get("accessToken").asText();

        // Register a normal user for access tests
        String uniqueEmail = "pkg_user" + System.currentTimeMillis() + "@test.dev";
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "secret123",
                  "fullName": "Pkg User"
                }
                """.formatted(uniqueEmail);

        String regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        userToken = objectMapper.readTree(regResult).get("accessToken").asText();
    }

    @Test
    void getActivePackagesShouldWork() throws Exception {
        mockMvc.perform(get("/api/v1/packages")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllPackagesShouldRequireAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/packages")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/packages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createPackageShouldWork() throws Exception {
        String createPayload = """
                {
                  "name": "TestPkg",
                  "coins": 9999,
                  "priceLabel": "$99.99",
                  "bonusLabel": "+2000 Bonus",
                  "sortOrder": 10
                }
                """;

        mockMvc.perform(post("/api/v1/admin/packages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("TestPkg"))
                .andExpect(jsonPath("$.coins").value(9999))
                .andExpect(jsonPath("$.priceLabel").value("$99.99"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createPackageWithInvalidDataShouldFail() throws Exception {
        String badPayload = """
                {
                  "name": "",
                  "coins": 0,
                  "priceLabel": ""
                }
                """;

        mockMvc.perform(post("/api/v1/admin/packages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disableAndEnablePackageShouldWork() throws Exception {
        // Create a package first
        String createPayload = """
                {
                  "name": "ToDisable",
                  "coins": 100,
                  "priceLabel": "$0.99",
                  "sortOrder": 99
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/packages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        long packageId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Disable it
        mockMvc.perform(put("/api/v1/admin/packages/" + packageId + "/disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Enable it back
        mockMvc.perform(put("/api/v1/admin/packages/" + packageId + "/enable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }
}
