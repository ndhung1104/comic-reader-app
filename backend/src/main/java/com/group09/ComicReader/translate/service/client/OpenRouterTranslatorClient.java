package com.group09.ComicReader.translate.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group09.ComicReader.config.properties.OpenRouterProperties;
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
public class OpenRouterTranslatorClient implements TranslatorClient {

    private static final String PROVIDER_KEY = "openrouter";

    private final OpenRouterProperties openRouterProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterTranslatorClient(OpenRouterProperties openRouterProperties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.openRouterProperties = openRouterProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public String getCacheIdentity() {
        return openRouterProperties.getModel();
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) {
        String apiKey = normalize(openRouterProperties.getApiKey());
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("OpenRouter API key not configured");
        }

        String model = normalize(openRouterProperties.getModel());
        if (model.isEmpty()) {
            throw new IllegalStateException("OpenRouter model is not configured");
        }

        String baseUrl = normalize(openRouterProperties.getBaseUrl());
        if (baseUrl.isEmpty()) {
            throw new IllegalStateException("OpenRouter base URL is not configured");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String prompt = String.format(
                "Translate the following text from %s to %s. Return ONLY the translated text, nothing else:\n\n%s",
                "auto".equalsIgnoreCase(sourceLang) ? "the original language" : sourceLang,
                targetLang,
                text
        );

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a professional translator. Return only translated text."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String siteUrl = normalize(openRouterProperties.getSiteUrl());
        if (!siteUrl.isEmpty()) {
            headers.set("HTTP-Referer", siteUrl);
        }
        String appName = normalize(openRouterProperties.getAppName());
        if (!appName.isEmpty()) {
            headers.set("X-Title", appName);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/chat/completions",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("OpenRouter returned non-success response");
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode contentNode = choices.get(0).path("message").path("content");
                String translated = extractMessageContent(contentNode);
                if (!translated.isEmpty()) {
                    return translated;
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse OpenRouter response", exception);
        }

        throw new IllegalStateException("OpenRouter response contained no translation text");
    }

    private String extractMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("").trim();
        }
        if (contentNode.isObject()) {
            return contentNode.path("text").asText("").trim();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String part;
                if (item.isTextual()) {
                    part = item.asText("").trim();
                } else {
                    part = item.path("text").asText("").trim();
                }
                if (part.isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(part);
            }
            return builder.toString().trim();
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
