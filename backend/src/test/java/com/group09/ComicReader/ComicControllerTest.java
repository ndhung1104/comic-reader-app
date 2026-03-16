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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetComicsWithUserToken() throws Exception {
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

        String token = objectMapper.readTree(loginResult).get("accessToken").asText();

        String comicsResult = mockMvc.perform(get("/api/v1/comics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode comicsJson = objectMapper.readTree(comicsResult);
        assertThat(comicsJson.get("content").isArray()).isTrue();
    }

    @Test
    void shouldGetComicChaptersWithoutToken() throws Exception {
        String chaptersResult = mockMvc.perform(get("/api/v1/comics/1/chapters"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode chaptersJson = objectMapper.readTree(chaptersResult);
        assertThat(chaptersJson.isArray()).isTrue();
        assertThat(chaptersJson.size()).isGreaterThan(0);
        assertThat(chaptersJson.get(0).get("unlocked").asBoolean()).isTrue();
    }

    @Test
    void shouldGetFreeChapterPagesWithoutToken() throws Exception {
        String pagesResult = mockMvc.perform(get("/api/v1/chapters/1/pages"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode pagesJson = objectMapper.readTree(pagesResult);
        assertThat(pagesJson.isArray()).isTrue();
        assertThat(pagesJson.size()).isGreaterThan(0);
    }
}

