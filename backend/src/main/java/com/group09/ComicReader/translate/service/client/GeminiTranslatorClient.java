package com.group09.ComicReader.translate.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group09.ComicReader.config.GeminiConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class GeminiTranslatorClient implements TranslatorClient {

    private static final String PROVIDER_KEY = "gemini";
    private static final String MODEL = "gemini-2.5-flash";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=%s";

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiTranslatorClient(GeminiConfig geminiConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.geminiConfig = geminiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public String getCacheIdentity() {
        return MODEL;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) {
        String apiKey = geminiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key not configured");
        }

        String prompt = String.format(
                "Translate the following text from %s to %s. Return ONLY the translated text, nothing else:\n\n%s",
                "auto".equals(sourceLang) ? "the original language" : sourceLang,
                targetLang,
                text
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = String.format(GEMINI_URL, apiKey);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Gemini returned non-success response");
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String translated = parts.get(0).path("text").asText("").trim();
                    if (!translated.isEmpty()) {
                        return translated;
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse Gemini response", exception);
        }

        throw new IllegalStateException("Gemini response contained no translation text");
    }
}
