package com.group09.ComicReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.wallet.entity.TopUpPackageEntity;
import com.group09.ComicReader.wallet.repository.TopUpPackageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComicRepository comicRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private TopUpPackageRepository topUpPackageRepository;

    @Test
    void iapVerifyShouldBeIdempotentForSameReference() throws Exception {
        String token = registerAndGetToken();
        TopUpPackageEntity topUpPackage = createTopUpPackage(700);

        String payload = """
                {
                  "store": "GOOGLE",
                  "packageId": %d,
                  "purchaseToken": "sandbox-token-idempotent",
                  "orderId": "sandbox-order-idempotent"
                }
                """.formatted(topUpPackage.getId());

        String firstResult = mockMvc.perform(post("/api/v1/wallet/iap-verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstWallet = objectMapper.readTree(firstResult);
        assertThat(firstWallet.get("coinBalance").asInt()).isEqualTo(700);

        String secondResult = mockMvc.perform(post("/api/v1/wallet/iap-verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode secondWallet = objectMapper.readTree(secondResult);
        assertThat(secondWallet.get("coinBalance").asInt()).isEqualTo(700);

        String txResult = mockMvc.perform(get("/api/v1/wallet/transactions?page=0&size=20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode txContent = objectMapper.readTree(txResult).get("content");
        assertThat(txContent).hasSize(1);
        assertThat(txContent.get(0).get("referenceId").asText()).isEqualTo("iap-sandbox-order-idempotent");
    }

    @Test
    void purchasePremiumChapterShouldUnlockAndRejectDuplicatePurchase() throws Exception {
        String token = registerAndGetToken();
        ChapterEntity premiumChapter = createPremiumChapter(120);

        String topUpPayload = """
                {
                  "store": "GOOGLE",
                  "productId": "coins_500",
                  "purchaseToken": "sandbox-token-purchase",
                  "orderId": "sandbox-order-purchase"
                }
                """;

        mockMvc.perform(post("/api/v1/wallet/iap-verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topUpPayload))
                .andExpect(status().isOk());

        String purchasePayload = """
                {
                  "chapterId": %d,
                  "currency": "COIN"
                }
                """.formatted(premiumChapter.getId());

        String purchaseResult = mockMvc.perform(post("/api/v1/wallet/purchase-chapter")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchasePayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode purchaseWallet = objectMapper.readTree(purchaseResult);
        assertThat(purchaseWallet.get("coinBalance").asInt()).isEqualTo(380);

        String chapterResult = mockMvc.perform(get("/api/v1/chapters/" + premiumChapter.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(chapterResult).get("unlocked").asBoolean()).isTrue();

        String listResult = mockMvc.perform(get("/api/v1/comics/" + premiumChapter.getComic().getId() + "/chapters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode chapters = objectMapper.readTree(listResult);
        boolean foundUnlocked = false;
        for (JsonNode chapter : chapters) {
            if (chapter.get("id").asLong() == premiumChapter.getId()) {
                foundUnlocked = chapter.get("unlocked").asBoolean();
                break;
            }
        }
        assertThat(foundUnlocked).isTrue();

        String duplicateResult = mockMvc.perform(post("/api/v1/wallet/purchase-chapter")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchasePayload))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(duplicateResult).get("error").asText())
                .contains("already purchased");
    }

    @Test
    void purchasePremiumChapterShouldFailWhenBalanceInsufficient() throws Exception {
        String token = registerAndGetToken();
        ChapterEntity premiumChapter = createPremiumChapter(250);

        String purchasePayload = """
                {
                  "chapterId": %d,
                  "currency": "COIN"
                }
                """.formatted(premiumChapter.getId());

        String result = mockMvc.perform(post("/api/v1/wallet/purchase-chapter")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchasePayload))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(result).get("error").asText())
                .contains("Insufficient coin balance");
    }

    private String registerAndGetToken() throws Exception {
        String email = "wallet-" + System.nanoTime() + "@comicreader.dev";
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "user123",
                                                                        "fullName": "Wallet Test",
                                                                        "dateOfBirth": "2000-01-01"
                }
                """.formatted(email);

        String registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(registerResult).get("accessToken").asText();
    }

    private ChapterEntity createPremiumChapter(int price) {
        ComicEntity comic = comicRepository.findAll().stream()
                .min(Comparator.comparingLong(ComicEntity::getId))
                .orElseThrow(() -> new IllegalStateException("No comic found for test setup"));

        int nextChapterNumber = chapterRepository.countByComicId(comic.getId()) + 100;
        ChapterEntity chapter = new ChapterEntity();
        chapter.setComic(comic);
        chapter.setChapterNumber(nextChapterNumber);
        chapter.setTitle("Premium Test Chapter " + System.nanoTime());
        chapter.setPremium(true);
        chapter.setPrice(price);
        return chapterRepository.save(chapter);
    }

    private TopUpPackageEntity createTopUpPackage(int coins) {
        TopUpPackageEntity topUpPackage = new TopUpPackageEntity();
        topUpPackage.setName("Mock package " + System.nanoTime());
        topUpPackage.setCoins(coins);
        topUpPackage.setPriceLabel("$0.99");
        topUpPackage.setBonusLabel("");
        topUpPackage.setActive(true);
        topUpPackage.setSortOrder(1);
        return topUpPackageRepository.save(topUpPackage);
    }
}
