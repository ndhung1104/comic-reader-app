package com.group09.ComicReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndLoginShouldReturnJwtToken() throws Exception {
        String uniqueEmail = "user" + System.currentTimeMillis() + "@test.dev";

        String registerPayload = """
                {
                  "email": "%s",
                  "password": "secret123",
                  "fullName": "Test User"
                }
                """.formatted(uniqueEmail);

        String registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResult);
        assertThat(registerJson.get("accessToken").asText()).isNotBlank();

        String loginPayload = """
                {
                  "email": "%s",
                  "password": "secret123"
                }
                """.formatted(uniqueEmail);

        String loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResult);
        assertThat(loginJson.get("accessToken").asText()).isNotBlank();
        assertThat(loginJson.get("tokenType").asText()).isEqualTo("Bearer");
    }

    @Test
    void registerDuplicateEmailShouldReturnBadRequestWithMessage() throws Exception {
        String email = "dup" + System.currentTimeMillis() + "@test.dev";

        String payload = """
                {
                  "email": "%s",
                  "password": "secret123",
                  "fullName": "Test User"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }
}

