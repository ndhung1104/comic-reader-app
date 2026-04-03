package com.group09.ComicReader.translate.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group09.ComicReader.config.properties.TranslateProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class LocalLlmTranslatorClient implements TranslatorClient {

    private static final String PROVIDER_KEY = "local";

    private final TranslateProperties translateProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LocalLlmTranslatorClient(TranslateProperties translateProperties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.translateProperties = translateProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public String getCacheIdentity() {
        return translateProperties.getLocal().getModel();
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) {
        TranslateProperties.Local local = translateProperties.getLocal();
        if (local.getBaseUrl() == null || local.getBaseUrl().isBlank()) {
            throw new IllegalStateException("Local LLM base URL not configured");
        }

        String baseUrl = local.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String source = sourceLang == null || sourceLang.isBlank() ? "auto" : sourceLang;
        String target = targetLang == null || targetLang.isBlank() ? "vi" : targetLang;

        String systemPrompt = "You are a professional translator. Return only translated text.";
        String userPrompt = String.format(
                "Translate the following text from %s to %s. Return ONLY the translated text, nothing else:\n\n%s",
                "auto".equalsIgnoreCase(source) ? "the original language" : source,
                target,
                text
        );

        Map<String, Object> body = Map.of(
                "model", local.getModel(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/api/chat", HttpMethod.POST, entity, String.class);
        if (Duration.between(start, Instant.now()).toMillis() > local.getTimeoutMs()) {
            throw new IllegalStateException("Local LLM request timed out");
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Local LLM returned non-success response");
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode messageContent = root.path("message").path("content");
            String translated = messageContent.asText("").trim();
            if (!translated.isEmpty()) {
                return translated;
            }

            JsonNode responseNode = root.path("response");
            translated = responseNode.asText("").trim();
            if (!translated.isEmpty()) {
                return translated;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse local LLM response", exception);
        }

        throw new IllegalStateException("Local LLM response contained no translation text");
    }
}
