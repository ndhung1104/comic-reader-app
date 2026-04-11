package com.group09.ComicReader.translate.service;

import com.group09.ComicReader.common.exception.ServiceUnavailableException;
import com.group09.ComicReader.config.GeminiConfig;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.translate.dto.ComicTranslateResponse;
import com.group09.ComicReader.translate.dto.TranslateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TranslateService {

    private static final Logger log = LoggerFactory.getLogger(TranslateService.class);
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
            throw new ServiceUnavailableException("Translation service is not configured on server");
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
                        String translatedText = parts.get(0).path("text").asText("").trim();
                        if (!translatedText.isEmpty()) {
                            return translatedText;
                        }
                    }
                }
            }

            log.warn("Gemini returned an empty or invalid translation response");
            throw new ServiceUnavailableException("Translation service returned an invalid response");
        } catch (ResourceAccessException e) {
            log.warn("Failed to reach Gemini translation service", e);
            throw new ServiceUnavailableException("Translation service is temporarily unavailable");
        } catch (RestClientResponseException e) {
            log.warn("Gemini translation request failed with status {}", e.getStatusCode(), e);
            throw new ServiceUnavailableException("Translation service request failed");
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unexpected error while translating with Gemini", e);
            throw new ServiceUnavailableException("Translation service failed to process the request");
        }
    }
}
