
package com.group09.ComicReader.ai.service.client;

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
public class OpenRouterAiClient implements AiClient {

    private final OpenRouterProperties openRouterProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterAiClient(OpenRouterProperties openRouterProperties,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper) {
        this.openRouterProperties = openRouterProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderKey() {
        return "openrouter";
    }

    @Override
    public String getModel() {
        return openRouterProperties.getModel();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String apiKey = normalize(openRouterProperties.getApiKey());
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("OpenRouter API key not configured");
        }

        String model = normalize(openRouterProperties.getModel());
        String baseUrl = normalize(openRouterProperties.getBaseUrl());
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
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
            String errorMessage = extractOpenRouterError(response);
            throw new IllegalStateException(errorMessage);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenRouter response", e);
        }
    }

    private String extractOpenRouterError(ResponseEntity<String> response) {
        String body = response.getBody();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode errorNode = root.path("error");
                if (errorNode.isObject()) {
                    String msg = errorNode.path("message").asText(null);
                    if (msg != null && !msg.isBlank()) {
                        return msg;
                    }
                }
                String msg = root.path("message").asText(null);
                if (msg != null && !msg.isBlank()) {
                    return msg;
                }
            } catch (Exception ignored) {
            }
            return "OpenRouter error: " + body;
        }
        return "OpenRouter returned non-success response: " + response.getStatusCode();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
