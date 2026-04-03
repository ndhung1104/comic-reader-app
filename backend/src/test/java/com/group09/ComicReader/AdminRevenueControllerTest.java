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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRevenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Login as admin (seeded by DataSeeder)
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

        JsonNode loginJson = objectMapper.readTree(loginResult);
        adminToken = loginJson.get("accessToken").asText();
    }

    @Test
    void getSummaryShouldReturnRevenueData() throws Exception {
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/v1/admin/revenue/summary")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTopUp").isNumber())
                .andExpect(jsonPath("$.totalPurchase").isNumber())
                .andExpect(jsonPath("$.totalVip").isNumber())
                .andExpect(jsonPath("$.totalRevenue").isNumber())
                .andExpect(jsonPath("$.transactionCount").isNumber());
    }

    @Test
    void getDailyRevenueShouldReturnDailyBreakdown() throws Exception {
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/v1/admin/revenue/daily")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].topUp").isNumber())
                .andExpect(jsonPath("$[0].purchase").isNumber())
                .andExpect(jsonPath("$[0].total").isNumber());
    }

    @Test
    void revenueShouldRequireAdminRole() throws Exception {
        // Register a normal user
        String uniqueEmail = "revenue_user" + System.currentTimeMillis() + "@test.dev";
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "secret123",
                  "fullName": "Rev User"
                }
                """.formatted(uniqueEmail);

        String regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userToken = objectMapper.readTree(regResult).get("accessToken").asText();

        String today = LocalDate.now().toString();
        mockMvc.perform(get("/api/v1/admin/revenue/summary")
                        .header("Authorization", "Bearer " + userToken)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isForbidden());
    }
}
