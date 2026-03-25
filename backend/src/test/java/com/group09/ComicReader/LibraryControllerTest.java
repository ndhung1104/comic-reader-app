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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void followAndReadingHistoryShouldAppearInLibrary() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(post("/api/v1/library/followed/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String followedResult = mockMvc.perform(get("/api/v1/library/followed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode followedJson = objectMapper.readTree(followedResult);
        assertThat(followedJson.isArray()).isTrue();
        boolean followedComicFound = false;
        for (JsonNode node : followedJson) {
            if (node.get("comicId").asLong() == 1L) {
                followedComicFound = true;
                break;
            }
        }
        assertThat(followedComicFound).isTrue();

        String historyPayload = """
                {
                  "comicId": 1,
                  "chapterId": 1,
                  "pageNumber": 2
                }
                """;

        mockMvc.perform(post("/api/v1/library/history")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyPayload))
                .andExpect(status().isOk());

        String recentResult = mockMvc.perform(get("/api/v1/library/recent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode recentJson = objectMapper.readTree(recentResult);
        assertThat(recentJson.isArray()).isTrue();
        assertThat(recentJson).isNotEmpty();
        assertThat(recentJson.get(0).get("comicId").asLong()).isEqualTo(1L);
        assertThat(recentJson.get(0).get("chapterId").asLong()).isEqualTo(1L);
        assertThat(recentJson.get(0).get("pageNumber").asInt()).isEqualTo(2);

        mockMvc.perform(delete("/api/v1/library/followed/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken() throws Exception {
        String loginPayload = """
                {
                  "email": "user@comicreader.dev",
                  "password": "user123"
                }
                """;

        String loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(loginResult).get("accessToken").asText();
    }
}
