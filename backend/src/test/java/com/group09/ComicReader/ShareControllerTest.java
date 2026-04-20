package com.group09.ComicReader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shareComicShouldReturnPreviewMetadataAndAppLinks() throws Exception {
        String html = mockMvc.perform(get("/share/comic/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("og:title");
        assertThat(html).contains("og:site_name");
        assertThat(html).contains("al:android:url");
        assertThat(html).contains("comicreader://comic/1");
        assertThat(html).contains("Shared from ComicReader");
    }

    @Test
    void shareChapterShouldReturnPreviewMetadataAndChapterDeepLink() throws Exception {
        String html = mockMvc.perform(get("/share/chapter/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("og:title");
        assertThat(html).contains("al:android:url");
        assertThat(html).contains("comicreader://chapter/1");
        assertThat(html).contains("chapterNumber=");
        assertThat(html).contains("If the app is not installed yet");
    }
}
