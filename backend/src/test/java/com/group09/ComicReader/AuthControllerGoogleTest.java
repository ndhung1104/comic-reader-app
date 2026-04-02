package com.group09.ComicReader;

import com.group09.ComicReader.auth.service.GoogleTokenVerifier;
import com.group09.ComicReader.auth.service.GoogleUserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerGoogleTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @Test
    void googleLoginShouldReturnJwtToken() throws Exception {
        String email = "google" + System.currentTimeMillis() + "@test.dev";
        when(googleTokenVerifier.verify(anyString()))
                .thenReturn(new GoogleUserInfo(email, "Google User", true));

        String payload = """
                {
                  \"idToken\": \"fake-token\"
                }
                """;

        String result = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(result).contains("accessToken");
        assertThat(result).contains("Bearer");
    }
}
