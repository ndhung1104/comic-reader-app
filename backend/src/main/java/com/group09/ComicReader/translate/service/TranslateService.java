package com.group09.ComicReader.translate.service;

import com.group09.ComicReader.config.GeminiConfig;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.translate.dto.ComicTranslateResponse;
import com.group09.ComicReader.translate.dto.TranslateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TranslateService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=%s";

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final ComicService comicService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* Simple in-memory cache: key = "text|sourceLang|targetLang" */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public TranslateService(GeminiConfig geminiConfig, RestTemplate restTemplate, ComicService comicService) {
        this.geminiConfig = geminiConfig;
        this.restTemplate = restTemplate;
        this.comicService = comicService;
    }

    public TranslateResponse translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return new TranslateResponse(text, "", sourceLang, targetLang);
        }
        String cacheKey = text + "|" + sourceLang + "|" + targetLang;
        String translated = cache.get(cacheKey);
        if (translated == null) {
            translated = callGemini(text, sourceLang, targetLang);
            cache.put(cacheKey, translated);
        }
        return new TranslateResponse(text, translated, sourceLang, targetLang);
    }

    public ComicTranslateResponse translateComic(Long comicId, String targetLang) {
        ComicEntity comic = comicService.getComicEntity(comicId);
        String translatedTitle = translate(comic.getTitle(), "auto", targetLang).getTranslatedText();
        String translatedSynopsis = translate(comic.getSynopsis(), "auto", targetLang).getTranslatedText();
        return new ComicTranslateResponse(comicId, translatedTitle, translatedSynopsis, targetLang);
    }

    private String callGemini(String text, String sourceLang, String targetLang) {
        String apiKey = geminiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "[Translation unavailable – Gemini API key not configured]";
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

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = String.format(GEMINI_URL, apiKey);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        return parts.get(0).path("text").asText("").trim();
                    }
                }
            }
            return "[Translation failed]";
        } catch (Exception e) {
            return "[Translation error: " + e.getMessage() + "]";
        }
    }
}
