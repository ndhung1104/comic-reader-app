package com.group09.ComicReader.translate.service;

import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.config.properties.TranslateProperties;
import com.group09.ComicReader.translate.dto.ComicTranslateResponse;
import com.group09.ComicReader.translate.dto.TranslateResponse;
import com.group09.ComicReader.translate.service.client.TranslatorClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TranslateService {

    private static final String NONE_PROVIDER = "none";

    private final ComicService comicService;
    private final TranslateProperties translateProperties;
    private final Map<String, TranslatorClient> translatorClients;

    /* Simple in-memory cache: key = "provider|model|text|sourceLang|targetLang" */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public TranslateService(ComicService comicService,
            TranslateProperties translateProperties,
            List<TranslatorClient> clients) {
        this.comicService = comicService;
        this.translateProperties = translateProperties;
        this.translatorClients = clients.stream()
                .collect(Collectors.toMap(
                        client -> normalizeProvider(client.getProviderKey()),
                        client -> client
                ));
    }

    public TranslateResponse translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return new TranslateResponse(text, "", sourceLang, targetLang);
        }

        String normalizedSource = sourceLang == null || sourceLang.isBlank() ? "auto" : sourceLang;
        String normalizedTarget = targetLang == null || targetLang.isBlank() ? "vi" : targetLang;

        TranslatorClient primaryClient = resolveProvider(translateProperties.getProvider(), true);
        String fallbackProvider = normalizeProvider(translateProperties.getFallbackProvider());
        TranslatorClient fallbackClient = null;
        if (!NONE_PROVIDER.equals(fallbackProvider) && !fallbackProvider.equals(normalizeProvider(primaryClient.getProviderKey()))) {
            fallbackClient = resolveProvider(fallbackProvider, false);
        }

        String cacheKey = buildCacheKey(text, normalizedSource, normalizedTarget, primaryClient, fallbackClient);
        String translated = cache.get(cacheKey);
        if (translated == null) {
            translated = executeTranslation(text, normalizedSource, normalizedTarget, primaryClient, fallbackClient);
            cache.put(cacheKey, translated);
        }

        return new TranslateResponse(text, translated, normalizedSource, normalizedTarget);
    }

    public ComicTranslateResponse translateComic(Long comicId, String targetLang) {
        ComicEntity comic = comicService.getComicEntity(comicId);
        String normalizedTarget = targetLang == null || targetLang.isBlank() ? "vi" : targetLang;

        String translatedTitle = translate(comic.getTitle(), "auto", normalizedTarget).getTranslatedText();
        String translatedSynopsis = translate(comic.getSynopsis(), "auto", normalizedTarget).getTranslatedText();
        return new ComicTranslateResponse(comicId, translatedTitle, translatedSynopsis, normalizedTarget);
    }

    private String executeTranslation(String text,
            String sourceLang,
            String targetLang,
            TranslatorClient primaryClient,
            TranslatorClient fallbackClient) {

        try {
            return primaryClient.translate(text, sourceLang, targetLang);
        } catch (Exception primaryException) {
            if (fallbackClient != null) {
                try {
                    return fallbackClient.translate(text, sourceLang, targetLang);
                } catch (Exception fallbackException) {
                    return "[Translation error: " + safeMessage(primaryException) + " | fallback: "
                            + safeMessage(fallbackException) + "]";
                }
            }
            return "[Translation error: " + safeMessage(primaryException) + "]";
        }
    }

    private TranslatorClient resolveProvider(String provider, boolean required) {
        String normalized = normalizeProvider(provider);
        if (NONE_PROVIDER.equals(normalized)) {
            if (required) {
                throw new BadRequestException("Primary translate provider cannot be 'none'");
            }
            return null;
        }

        TranslatorClient client = translatorClients.get(normalized);
        if (client == null && required) {
            throw new BadRequestException("Unsupported translate provider: " + provider);
        }
        return client;
    }

    private String buildCacheKey(String text,
            String sourceLang,
            String targetLang,
            TranslatorClient primaryClient,
            TranslatorClient fallbackClient) {

        String primary = primaryClient.getProviderKey() + ":" + primaryClient.getCacheIdentity();
        String fallback = fallbackClient == null
                ? NONE_PROVIDER
                : fallbackClient.getProviderKey() + ":" + fallbackClient.getCacheIdentity();

        return primary + "|" + fallback + "|" + sourceLang + "|" + targetLang + "|" + text;
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return NONE_PROVIDER;
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String safeMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "unknown error";
        }
        return exception.getMessage();
    }
}
