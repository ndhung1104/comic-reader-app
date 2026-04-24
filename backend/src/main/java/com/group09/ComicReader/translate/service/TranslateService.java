package com.group09.ComicReader.translate.service;

import com.group09.ComicReader.ai.entity.AiFeature;
import com.group09.ComicReader.ai.service.AiUsageService;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.config.properties.TranslateProperties;
import com.group09.ComicReader.translate.dto.ComicTranslateResponse;
import com.group09.ComicReader.translate.dto.TranslateResponse;
import com.group09.ComicReader.translate.service.client.TranslatorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TranslateService {

    private static final String NONE_PROVIDER = "none";
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslateService.class);

    private final ComicService comicService;
    private final TranslateProperties translateProperties;
    private final Map<String, TranslatorClient> translatorClients;
    private final AiUsageService aiUsageService;

    /* Simple in-memory cache: key = "provider|model|text|sourceLang|targetLang" */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public TranslateService(ComicService comicService,
            TranslateProperties translateProperties,
            AiUsageService aiUsageService,
            List<TranslatorClient> clients) {
        this.comicService = comicService;
        this.translateProperties = translateProperties;
        this.aiUsageService = aiUsageService;
        this.translatorClients = clients.stream()
                .collect(Collectors.toMap(
                        client -> normalizeProvider(client.getProviderKey()),
                        client -> client
                ));
    }

    public TranslateResponse translate(String text, String sourceLang, String targetLang) {
        AiUsageService.UsageContext usageContext = aiUsageService.beginUsage(
                AiFeature.TEXT_TRANSLATE,
                null,
                text == null ? 0 : text.length(),
                "targetLang=" + targetLang
        );
        TranslationExecutionResult executionResult = null;
        try {
            executionResult = translateTextInternal(text, sourceLang, targetLang);
            if (executionResult.failed()) {
                aiUsageService.completeFailure(usageContext,
                        executionResult.provider(),
                        executionResult.model(),
                        executionResult.details());
            } else {
                aiUsageService.completeSuccess(usageContext,
                        executionResult.provider(),
                        executionResult.model(),
                        executionResult.translatedText() == null ? 0 : executionResult.translatedText().length(),
                        executionResult.details());
            }
            return new TranslateResponse(
                    executionResult.originalText(),
                    executionResult.translatedText(),
                    executionResult.sourceLang(),
                    executionResult.targetLang()
            );
        } catch (RuntimeException exception) {
            aiUsageService.completeFailure(usageContext, null, null, safeMessage(exception));
            throw exception;
        }
    }

    public ComicTranslateResponse translateComic(Long comicId, String targetLang) {
        ComicEntity comic = comicService.getComicEntity(comicId);
        String normalizedTarget = targetLang == null || targetLang.isBlank() ? "vi" : targetLang;

        int requestUnits = lengthOf(comic.getTitle()) + lengthOf(comic.getSynopsis());
        AiUsageService.UsageContext usageContext = aiUsageService.beginUsage(
                AiFeature.TEXT_TRANSLATE,
                "comic-" + comicId,
                requestUnits,
                "translateComic"
        );

        TranslationExecutionResult titleResult = null;
        TranslationExecutionResult synopsisResult = null;
        try {
            titleResult = translateTextInternal(comic.getTitle(), "auto", normalizedTarget);
            synopsisResult = translateTextInternal(comic.getSynopsis(), "auto", normalizedTarget);

            boolean failed = titleResult.failed() || synopsisResult.failed();
            String provider = synopsisResult.provider() != null ? synopsisResult.provider() : titleResult.provider();
            String model = synopsisResult.model() != null ? synopsisResult.model() : titleResult.model();
            String details = (titleResult.details() == null ? "" : titleResult.details())
                    + (synopsisResult.details() == null ? "" : " | " + synopsisResult.details());
            if (failed) {
                aiUsageService.completeFailure(usageContext, provider, model, details);
            } else {
                aiUsageService.completeSuccess(
                        usageContext,
                        provider,
                        model,
                        lengthOf(titleResult.translatedText()) + lengthOf(synopsisResult.translatedText()),
                        details
                );
            }

            return new ComicTranslateResponse(
                    comicId,
                    titleResult.translatedText(),
                    synopsisResult.translatedText(),
                    normalizedTarget
            );
        } catch (RuntimeException exception) {
            aiUsageService.completeFailure(usageContext, null, null, safeMessage(exception));
            throw exception;
        }
    }

    private TranslationExecutionResult translateTextInternal(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            String normalizedSource = sourceLang == null || sourceLang.isBlank() ? "auto" : sourceLang;
            String normalizedTarget = targetLang == null || targetLang.isBlank() ? "vi" : targetLang;
            return new TranslationExecutionResult(text, "", normalizedSource, normalizedTarget, null, null, false, "empty_text");
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
        TranslationExecutionResult executionResult;
        if (translated == null) {
            executionResult = executeTranslation(text, normalizedSource, normalizedTarget, primaryClient, fallbackClient);
            cache.put(cacheKey, executionResult.translatedText());
        } else {
            executionResult = new TranslationExecutionResult(
                    text,
                    translated,
                    normalizedSource,
                    normalizedTarget,
                    primaryClient.getProviderKey(),
                    primaryClient.getCacheIdentity(),
                    false,
                    "cache_hit"
            );
        }

        return executionResult;
    }

    private TranslationExecutionResult executeTranslation(String text,
            String sourceLang,
            String targetLang,
            TranslatorClient primaryClient,
            TranslatorClient fallbackClient) {

        try {
            String translated = primaryClient.translate(text, sourceLang, targetLang);
            LOGGER.info("translate_success provider={} targetLang={} sourceLang={}",
                    primaryClient.getProviderKey(),
                    targetLang,
                    sourceLang);
            return new TranslationExecutionResult(
                    text,
                    translated,
                    sourceLang,
                    targetLang,
                    primaryClient.getProviderKey(),
                    primaryClient.getCacheIdentity(),
                    false,
                    "primary"
            );
        } catch (Exception primaryException) {
            if (fallbackClient != null) {
                try {
                    String translated = fallbackClient.translate(text, sourceLang, targetLang);
                    LOGGER.info("translate_fallback_success primary={} fallback={} targetLang={} sourceLang={}",
                            primaryClient.getProviderKey(),
                            fallbackClient.getProviderKey(),
                            targetLang,
                            sourceLang);
                    return new TranslationExecutionResult(
                            text,
                            translated,
                            sourceLang,
                            targetLang,
                            fallbackClient.getProviderKey(),
                            fallbackClient.getCacheIdentity(),
                            false,
                            "fallback_from=" + safeMessage(primaryException)
                    );
                } catch (Exception fallbackException) {
                    return new TranslationExecutionResult(
                            text,
                            "[Translation error: " + safeMessage(primaryException) + " | fallback: "
                                    + safeMessage(fallbackException) + "]",
                            sourceLang,
                            targetLang,
                            fallbackClient.getProviderKey(),
                            fallbackClient.getCacheIdentity(),
                            true,
                            "primary=" + safeMessage(primaryException) + " | fallback=" + safeMessage(fallbackException)
                    );
                }
            }
            return new TranslationExecutionResult(
                    text,
                    "[Translation error: " + safeMessage(primaryException) + "]",
                    sourceLang,
                    targetLang,
                    primaryClient.getProviderKey(),
                    primaryClient.getCacheIdentity(),
                    true,
                    safeMessage(primaryException)
            );
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

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private record TranslationExecutionResult(
            String originalText,
            String translatedText,
            String sourceLang,
            String targetLang,
            String provider,
            String model,
            boolean failed,
            String details) {
    }
}
