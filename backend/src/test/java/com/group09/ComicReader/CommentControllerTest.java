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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private String adminToken;
        private String userToken;

        @BeforeEach
        void setUp() throws Exception {
                adminToken = loginAndGetToken("admin@comicreader.dev", "admin123");
                userToken = loginAndGetToken("user@comicreader.dev", "user123");
        }

        private String loginAndGetToken(String email, String password) throws Exception {
                String payload = """
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password);

                String result = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                return objectMapper.readTree(result).get("accessToken").asText();
        }

        @Test
        void shouldGetCommentsPublicly() throws Exception {
                String result = mockMvc.perform(get("/api/v1/comics/1/comments"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode json = objectMapper.readTree(result);
                assertThat(json.isArray()).isTrue();
        }

        @Test
        void authenticatedUserShouldCreateComment() throws Exception {
                String commentPayload = """
                                {
                                "content": "Great comic!",
                                "sourceType": "SOCIAL_SHARE"
                                }
                                """;

                String result = mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentPayload))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode commentJson = objectMapper.readTree(result);
                assertThat(commentJson.get("content").asText()).isEqualTo("Great comic!");
                assertThat(commentJson.get("hidden").asBoolean()).isFalse();
                assertThat(commentJson.get("locked").asBoolean()).isFalse();
                assertThat(commentJson.get("sourceType").asText()).isEqualTo("SOCIAL_SHARE");
                assertThat(commentJson.get("comicId").asLong()).isEqualTo(1L);
        }

        @Test
        void unauthenticatedUserCannotCreateComment() throws Exception {
                String commentPayload = """
                                {
                                "content": "This should fail"
                                }
                                """;

                mockMvc.perform(post("/api/v1/comics/1/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentPayload))
                                .andExpect(status().isForbidden());
        }

        @Test
        void authenticatedUserCanReplyToCommentAndThreadOrderIsParentThenChild() throws Exception {
                // Create root comment
                String rootPayload = """
                                {
                                  "content": "Root comment"
                                }
                                """;

                String rootResult = mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rootPayload))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode rootJson = objectMapper.readTree(rootResult);
                long rootId = rootJson.get("id").asLong();

                // Create reply
                String replyPayload = """
                                {
                                  "content": "Child reply",
                                  "parentCommentId": %d
                                }
                                """.formatted(rootId);

                String replyResult = mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(replyPayload))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode replyJson = objectMapper.readTree(replyResult);
                assertThat(replyJson.get("parentCommentId").asLong()).isEqualTo(rootId);
                assertThat(replyJson.get("depth").asInt()).isEqualTo(1);
                assertThat(replyJson.get("rootCommentId").asLong()).isEqualTo(rootId);

                // Thread listing: parent must appear before child
                String pagedResult = mockMvc.perform(get("/api/v1/comics/1/comments/paged")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode content = objectMapper.readTree(pagedResult).get("content");
                assertThat(content.isArray()).isTrue();

                int rootIndex = -1;
                int childIndex = -1;
                for (int i = 0; i < content.size(); i++) {
                        JsonNode c = content.get(i);
                        if (c.get("id").asLong() == rootId) {
                                rootIndex = i;
                        }
                        if (c.hasNonNull("parentCommentId") && c.get("parentCommentId").asLong() == rootId
                                        && "Child reply".equals(c.get("content").asText())) {
                                childIndex = i;
                        }
                }

                assertThat(rootIndex).isGreaterThanOrEqualTo(0);
                assertThat(childIndex).isGreaterThanOrEqualTo(0);
                assertThat(childIndex).isGreaterThan(rootIndex);
        }

        @Test
        void adminShouldHideAndUnhideComment() throws Exception {
                // Create a comment as user
                String commentPayload = """
                                {
                                "content": "Comment to moderate"
                                }
                                """;

                String createResult = mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentPayload))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long commentId = objectMapper.readTree(createResult).get("id").asLong();

                // Admin hides comment
                String hideResult = mockMvc.perform(put("/api/v1/admin/comments/" + commentId
                                + "/hide")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                assertThat(objectMapper.readTree(hideResult).get("hidden").asBoolean()).isTrue();

                // Public list should not contain hidden comment
                String publicResult = mockMvc.perform(get("/api/v1/comics/1/comments"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode publicComments = objectMapper.readTree(publicResult);
                boolean foundHidden = false;
                for (JsonNode c : publicComments) {
                        if (c.get("id").asLong() == commentId) {
                                foundHidden = true;
                                break;
                        }
                }
                assertThat(foundHidden).isFalse();

                // Admin can see hidden comments
                String adminResult = mockMvc.perform(get("/api/v1/admin/comics/1/comments")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode adminComments = objectMapper.readTree(adminResult).get("content");
                boolean foundInAdmin = false;
                for (JsonNode c : adminComments) {
                        if (c.get("id").asLong() == commentId) {
                                foundInAdmin = true;
                                assertThat(c.get("hidden").asBoolean()).isTrue();
                                break;
                        }
                }
                assertThat(foundInAdmin).isTrue();

                // Admin unhides comment
                String unhideResult = mockMvc.perform(put("/api/v1/admin/comments/" +
                                commentId + "/unhide")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                assertThat(objectMapper.readTree(unhideResult).get("hidden").asBoolean()).isFalse();
        }

        @Test
        void adminShouldLockAndUnlockComment() throws Exception {
                // Create a comment as user
                String commentPayload = """
                                {
                                "content": "Comment to lock"
                                }
                                """;

                String createResult = mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentPayload))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long commentId = objectMapper.readTree(createResult).get("id").asLong();

                // Admin locks comment
                String lockResult = mockMvc.perform(put("/api/v1/admin/comments/" + commentId
                                + "/lock")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                assertThat(objectMapper.readTree(lockResult).get("locked").asBoolean()).isTrue();

                // Admin unlocks comment
                String unlockResult = mockMvc.perform(put("/api/v1/admin/comments/" +
                                commentId + "/unlock")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                assertThat(objectMapper.readTree(unlockResult).get("locked").asBoolean()).isFalse();
        }

        @Test
        void adminShouldDeleteComment() throws Exception {
                // Create a comment
                String commentPayload = """
                                {
                                "content": "To be deleted"
                                }
                                """;

                String createResult = mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentPayload))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long commentId = objectMapper.readTree(createResult).get("id").asLong();

                // Admin deletes
                mockMvc.perform(delete("/api/v1/admin/comments/" + commentId)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isNoContent());
        }

        @Test
        void userCannotCreateCommentWithInvalidSourceType() throws Exception {
                // Given
                String invalidPayload = """
                                {
                                        "content": "This is a hack comment",
                                        "sourceType": "HACK_SOURCE_INVALID"
                                }
                                """;

                // When
                mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidPayload))
                                // Then
                                .andExpect(status().isBadRequest());
        }

        @Test
        void distinguishCommentBySourceType() throws Exception {
                // Given
                String normalComment = """
                                {
                                        "content": "This is a normal comment",
                                        "sourceType": "NORMAL"
                                }
                                """;
                String socialShareComment = """
                                {
                                        "content": "This is a social share comment",
                                        "sourceType": "SOCIAL_SHARE"
                                }
                                """;
                // When
                mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(normalComment))
                                // Then
                                .andExpect(status().isCreated());

                // When
                mockMvc.perform(post("/api/v1/comics/1/comments")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(socialShareComment))
                                // Then
                                .andExpect(status().isCreated());

                // When
                String adminResult = mockMvc.perform(get("/api/v1/admin/comics/1/comments")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                // Then
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode jsonResponse = objectMapper.readTree(adminResult);
                JsonNode commentsList = jsonResponse.get("content");

                boolean foundNormal = false;
                boolean foundSocialShare = false;

                for (JsonNode comment : commentsList) {
                        String payloadContent = comment.get("content").asText();
                        String sourceType = comment.get("sourceType").asText();
                        if (payloadContent.equals("This is a normal comment")) {
                                assertThat(sourceType).isEqualTo("NORMAL");
                                foundNormal = true;
                        } else if (payloadContent.equals("This is a social share comment")) {
                                assertThat(sourceType).isEqualTo("SOCIAL_SHARE");
                                foundSocialShare = true;
                        }
                }

                assertThat(foundNormal).isTrue();
                assertThat(foundSocialShare).isTrue();
        }
}
