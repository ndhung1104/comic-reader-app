package com.group09.ComicReader.translate.service;

import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.config.properties.TranslateProperties;
import com.group09.ComicReader.translate.service.client.TranslatorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TranslateServiceTest {

    @Mock
    private ComicService comicService;

    private TranslateProperties translateProperties;
    private FakeTranslatorClient localClient;
    private FakeTranslatorClient geminiClient;
    private TranslateService translateService;

    @BeforeEach
    void setUp() {
        translateProperties = new TranslateProperties();
        translateProperties.setProvider("local");
        translateProperties.setFallbackProvider("none");

        localClient = new FakeTranslatorClient("local", "qwen2.5:1.5b");
        geminiClient = new FakeTranslatorClient("gemini", "gemini-2.5-flash");
        translateService = new TranslateService(comicService, translateProperties, List.of(localClient, geminiClient));
    }

    @Test
    void shouldUsePrimaryProviderWhenSuccess() {
        localClient.setResult("xin chao");

        String translated = translateService.translate("hello", "en", "vi").getTranslatedText();

        assertThat(translated).isEqualTo("xin chao");
        assertThat(localClient.getCallCount()).isEqualTo(1);
        assertThat(geminiClient.getCallCount()).isZero();
    }

    @Test
    void shouldFallbackWhenPrimaryFails() {
        localClient.setFailure(new IllegalStateException("local unavailable"));
        geminiClient.setResult("xin chao tu gemini");
        translateProperties.setFallbackProvider("gemini");

        String translated = translateService.translate("hello", "en", "vi").getTranslatedText();

        assertThat(translated).isEqualTo("xin chao tu gemini");
        assertThat(localClient.getCallCount()).isEqualTo(1);
        assertThat(geminiClient.getCallCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnErrorWhenBothProvidersFail() {
        localClient.setFailure(new IllegalStateException("local down"));
        geminiClient.setFailure(new IllegalStateException("gemini down"));
        translateProperties.setFallbackProvider("gemini");

        String translated = translateService.translate("hello", "en", "vi").getTranslatedText();

        assertThat(translated).contains("Translation error");
        assertThat(translated).contains("fallback");
    }

    @Test
    void cacheShouldBeSeparatedByProviderIdentity() {
        localClient.setResult("result-local");
        String first = translateService.translate("hello", "en", "vi").getTranslatedText();
        assertThat(first).isEqualTo("result-local");

        translateProperties.setProvider("gemini");
        translateProperties.setFallbackProvider("none");
        geminiClient.setResult("result-gemini");

        String second = translateService.translate("hello", "en", "vi").getTranslatedText();
        assertThat(second).isEqualTo("result-gemini");
    }

    private static class FakeTranslatorClient implements TranslatorClient {

        private final String providerKey;
        private final String cacheIdentity;
        private final AtomicInteger callCount = new AtomicInteger();

        private String result = "";
        private RuntimeException failure;

        private FakeTranslatorClient(String providerKey, String cacheIdentity) {
            this.providerKey = providerKey;
            this.cacheIdentity = cacheIdentity;
        }

        @Override
        public String getProviderKey() {
            return providerKey;
        }

        @Override
        public String getCacheIdentity() {
            return cacheIdentity;
        }

        @Override
        public String translate(String text, String sourceLang, String targetLang) {
            callCount.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            return result;
        }

        public void setResult(String result) {
            this.result = result;
            this.failure = null;
        }

        public void setFailure(RuntimeException failure) {
            this.failure = failure;
        }

        public int getCallCount() {
            return callCount.get();
        }
    }
}
