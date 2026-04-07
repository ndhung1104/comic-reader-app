package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.chapter.dto.ChapterAudioPageResponse;
import com.group09.ComicReader.chapter.dto.ChapterAudioPlaylistRequest;
import com.group09.ComicReader.chapter.dto.ChapterAudioPlaylistResponse;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.storage.FileStorageService;
import com.group09.ComicReader.config.properties.TtsWorkerProperties;
import com.group09.ComicReader.translationjob.client.TtsWorkerClient;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerAudioPage;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerPageInput;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchRequest;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchResponse;
import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import com.group09.ComicReader.translationjob.entity.ChapterPageTtsAudioEntity;
import com.group09.ComicReader.translationjob.repository.ChapterPageOcrTextRepository;
import com.group09.ComicReader.translationjob.repository.ChapterPageTtsAudioRepository;
import com.group09.ComicReader.translationjob.service.TranslationJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChapterAudioPlaylistService {

    private final ChapterService chapterService;
    private final ChapterPageRepository chapterPageRepository;
    private final ChapterPageOcrTextRepository chapterPageOcrTextRepository;
    private final ChapterPageTtsAudioRepository chapterPageTtsAudioRepository;
    private final TranslationJobService translationJobService;
    private final TtsWorkerClient ttsWorkerClient;
    private final TtsWorkerProperties ttsWorkerProperties;
    private final FileStorageService fileStorageService;

    public ChapterAudioPlaylistService(ChapterService chapterService,
            ChapterPageRepository chapterPageRepository,
            ChapterPageOcrTextRepository chapterPageOcrTextRepository,
            ChapterPageTtsAudioRepository chapterPageTtsAudioRepository,
            TranslationJobService translationJobService,
            TtsWorkerClient ttsWorkerClient,
            TtsWorkerProperties ttsWorkerProperties,
            FileStorageService fileStorageService) {
        this.chapterService = chapterService;
        this.chapterPageRepository = chapterPageRepository;
        this.chapterPageOcrTextRepository = chapterPageOcrTextRepository;
        this.chapterPageTtsAudioRepository = chapterPageTtsAudioRepository;
        this.translationJobService = translationJobService;
        this.ttsWorkerClient = ttsWorkerClient;
        this.ttsWorkerProperties = ttsWorkerProperties;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ChapterAudioPlaylistResponse createOrGetPlaylist(Long chapterId, ChapterAudioPlaylistRequest request) {
        // Reuse existing access-control and lazy-page-loading path.
        chapterService.getPages(chapterId);

        List<ChapterPageEntity> chapterPages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
        if (chapterPages.isEmpty()) {
            throw new BadRequestException("Chapter has no pages");
        }

        String sourceLang = normalizeLang(request == null ? null : request.getSourceLang(), ttsWorkerProperties.getDefaultLang());
        String lang = normalizeLang(request == null ? null : request.getLang(), sourceLang);
        String voice = resolveVoice(request == null ? null : request.getVoice(), lang);
        Double speed = normalizeSpeed(request == null ? null : request.getSpeed(), ttsWorkerProperties.getDefaultSpeed());

        List<ChapterPageTtsAudioEntity> existingAudio = chapterPageTtsAudioRepository
                .findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(chapterId, lang, voice, speed);
        Map<Integer, ChapterPageTtsAudioEntity> existingByPage = indexAudioByPage(existingAudio);

        if (isPlaylistReady(chapterPages.size(), existingByPage)) {
            return toPlaylistResponse(chapterId, "READY", sourceLang, lang, voice, speed, existingByPage);
        }

        List<ChapterPageOcrTextEntity> ocrPages = chapterPageOcrTextRepository
                .findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, sourceLang);
        if (ocrPages.size() < chapterPages.size()) {
            ocrPages = translationJobService.ensureChapterOcrText(chapterId, sourceLang);
        }

        Map<Integer, ChapterPageOcrTextEntity> ocrByPage = indexOcrByPage(ocrPages);
        List<TtsWorkerPageInput> pagesToGenerate = new ArrayList<>();
        Map<Integer, String> hashByPage = new HashMap<>();

        for (int index = 0; index < chapterPages.size(); index++) {
            int pageNumber = index + 1;
            ChapterPageOcrTextEntity ocrPage = ocrByPage.get(pageNumber);
            if (ocrPage == null || ocrPage.getOcrText() == null || ocrPage.getOcrText().isBlank()) {
                throw new BadRequestException("Missing OCR text for page " + pageNumber);
            }

            String textHash = hashText(ocrPage.getOcrText());
            hashByPage.put(pageNumber, textHash);
            ChapterPageTtsAudioEntity existing = existingByPage.get(pageNumber);
            if (existing != null
                    && existing.getAudioPath() != null
                    && !existing.getAudioPath().isBlank()
                    && textHash.equals(existing.getSourceTextHash())) {
                continue;
            }

            TtsWorkerPageInput input = new TtsWorkerPageInput();
            input.setPageNumber(pageNumber);
            input.setText(ocrPage.getOcrText());
            pagesToGenerate.add(input);
        }

        if (!pagesToGenerate.isEmpty()) {
            TtsWorkerSynthesizeBatchRequest workerRequest = new TtsWorkerSynthesizeBatchRequest();
            workerRequest.setChapterId(String.valueOf(chapterId));
            workerRequest.setLang(lang);
            workerRequest.setVoice(voice);
            workerRequest.setSpeed(speed);
            workerRequest.setPages(pagesToGenerate);

            TtsWorkerSynthesizeBatchResponse workerResponse = ttsWorkerClient.synthesizeBatch(workerRequest);
            if (!"SUCCEEDED".equalsIgnoreCase(workerResponse.getStatus())) {
                throw new BadRequestException("TTS worker failed: " + workerResponse.getError());
            }

            Map<Integer, TtsWorkerAudioPage> generatedByPage = indexGeneratedByPage(workerResponse.getAudioPages());
            for (TtsWorkerPageInput input : pagesToGenerate) {
                TtsWorkerAudioPage generated = generatedByPage.get(input.getPageNumber());
                if (generated == null || generated.getAudioBase64() == null || generated.getAudioBase64().isBlank()) {
                    throw new BadRequestException("TTS worker returned missing audio for page " + input.getPageNumber());
                }

                byte[] audioBytes;
                try {
                    audioBytes = Base64.getDecoder().decode(generated.getAudioBase64());
                } catch (IllegalArgumentException exception) {
                    throw new BadRequestException("Invalid TTS audio payload for page " + input.getPageNumber());
                }

                String audioPath = fileStorageService.storeChapterPageAudio(
                        chapterId,
                        input.getPageNumber(),
                        lang,
                        voice,
                        speed,
                        audioBytes
                );

                ChapterPageTtsAudioEntity entity = existingByPage.get(input.getPageNumber());
                if (entity == null) {
                    entity = new ChapterPageTtsAudioEntity();
                    entity.setChapter(chapterPages.get(0).getChapter());
                    entity.setPageNumber(input.getPageNumber());
                    entity.setLang(lang);
                    entity.setVoice(voice);
                    entity.setSpeed(speed);
                    entity.setCreatedAt(LocalDateTime.now());
                }

                entity.setAudioPath(audioPath);
                entity.setDurationMs(generated.getDurationMs());
                entity.setSourceTextHash(hashByPage.get(input.getPageNumber()));
                ChapterPageOcrTextEntity sourceOcr = ocrByPage.get(input.getPageNumber());
                entity.setSourceOcrJob(sourceOcr == null ? null : sourceOcr.getOcrJob());
                entity.setUpdatedAt(LocalDateTime.now());
                ChapterPageTtsAudioEntity saved = chapterPageTtsAudioRepository.save(entity);
                existingByPage.put(saved.getPageNumber(), saved);
            }
        }

        if (!isPlaylistReady(chapterPages.size(), existingByPage)) {
            throw new BadRequestException("Audio playlist is incomplete");
        }
        return toPlaylistResponse(chapterId, "READY", sourceLang, lang, voice, speed, existingByPage);
    }

    @Transactional(readOnly = true)
    public ChapterAudioPlaylistResponse getExistingPlaylist(Long chapterId, ChapterAudioPlaylistRequest request) {
        chapterService.getPages(chapterId);
        List<ChapterPageEntity> chapterPages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
        if (chapterPages.isEmpty()) {
            throw new BadRequestException("Chapter has no pages");
        }

        String sourceLang = normalizeLang(request == null ? null : request.getSourceLang(), ttsWorkerProperties.getDefaultLang());
        String lang = normalizeLang(request == null ? null : request.getLang(), sourceLang);
        String voice = resolveVoice(request == null ? null : request.getVoice(), lang);
        Double speed = normalizeSpeed(request == null ? null : request.getSpeed(), ttsWorkerProperties.getDefaultSpeed());

        List<ChapterPageTtsAudioEntity> existingAudio = chapterPageTtsAudioRepository
                .findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(chapterId, lang, voice, speed);
        Map<Integer, ChapterPageTtsAudioEntity> existingByPage = indexAudioByPage(existingAudio);
        String status = isPlaylistReady(chapterPages.size(), existingByPage) ? "READY" : "MISSING";
        return toPlaylistResponse(chapterId, status, sourceLang, lang, voice, speed, existingByPage);
    }

    private Map<Integer, ChapterPageTtsAudioEntity> indexAudioByPage(List<ChapterPageTtsAudioEntity> rows) {
        Map<Integer, ChapterPageTtsAudioEntity> byPage = new HashMap<>();
        if (rows == null) {
            return byPage;
        }
        for (ChapterPageTtsAudioEntity row : rows) {
            if (row == null || row.getPageNumber() == null || row.getPageNumber() <= 0) {
                continue;
            }
            byPage.put(row.getPageNumber(), row);
        }
        return byPage;
    }

    private Map<Integer, ChapterPageOcrTextEntity> indexOcrByPage(List<ChapterPageOcrTextEntity> rows) {
        Map<Integer, ChapterPageOcrTextEntity> byPage = new HashMap<>();
        if (rows == null) {
            return byPage;
        }
        for (ChapterPageOcrTextEntity row : rows) {
            if (row == null || row.getPageNumber() == null || row.getPageNumber() <= 0) {
                continue;
            }
            byPage.put(row.getPageNumber(), row);
        }
        return byPage;
    }

    private Map<Integer, TtsWorkerAudioPage> indexGeneratedByPage(List<TtsWorkerAudioPage> rows) {
        Map<Integer, TtsWorkerAudioPage> byPage = new HashMap<>();
        if (rows == null) {
            return byPage;
        }
        for (TtsWorkerAudioPage row : rows) {
            if (row == null || row.getPageNumber() == null || row.getPageNumber() <= 0) {
                continue;
            }
            byPage.put(row.getPageNumber(), row);
        }
        return byPage;
    }

    private boolean isPlaylistReady(int expectedPageCount, Map<Integer, ChapterPageTtsAudioEntity> byPage) {
        for (int pageNumber = 1; pageNumber <= expectedPageCount; pageNumber++) {
            ChapterPageTtsAudioEntity page = byPage.get(pageNumber);
            if (page == null || page.getAudioPath() == null || page.getAudioPath().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private ChapterAudioPlaylistResponse toPlaylistResponse(Long chapterId,
            String status,
            String sourceLang,
            String lang,
            String voice,
            Double speed,
            Map<Integer, ChapterPageTtsAudioEntity> audioByPage) {
        ChapterAudioPlaylistResponse response = new ChapterAudioPlaylistResponse();
        response.setChapterId(chapterId);
        response.setStatus(status);
        response.setSourceLang(sourceLang);
        response.setLang(lang);
        response.setVoice(voice);
        response.setSpeed(speed);

        List<ChapterAudioPageResponse> pages = new ArrayList<>();
        audioByPage.values().stream()
                .sorted(Comparator.comparing(ChapterPageTtsAudioEntity::getPageNumber))
                .forEach(entity -> {
                    if (entity.getAudioPath() == null || entity.getAudioPath().isBlank()) {
                        return;
                    }
                    ChapterAudioPageResponse page = new ChapterAudioPageResponse();
                    page.setPageNumber(entity.getPageNumber());
                    page.setAudioUrl(entity.getAudioPath());
                    page.setDurationMs(entity.getDurationMs());
                    pages.add(page);
                });
        response.setAudioPages(pages);
        return response;
    }

    private String normalizeLang(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveVoice(String requestedVoice, String lang) {
        if (requestedVoice != null && !requestedVoice.isBlank() && !"auto".equalsIgnoreCase(requestedVoice.trim())) {
            return requestedVoice.trim();
        }

        String normalizedLang = lang == null ? "" : lang.trim().toLowerCase(Locale.ROOT);
        if (isLang(normalizedLang, "vi", "vietnamese")) {
            return fallbackVoice(ttsWorkerProperties.getVoiceVi());
        }
        if (isLang(normalizedLang, "ko", "korean")) {
            return fallbackVoice(ttsWorkerProperties.getVoiceKo());
        }
        if (isLang(normalizedLang, "ja", "japanese")) {
            return fallbackVoice(ttsWorkerProperties.getVoiceJa());
        }
        if (isLang(normalizedLang, "en", "english")) {
            return fallbackVoice(ttsWorkerProperties.getVoiceEn());
        }

        return fallbackVoice(ttsWorkerProperties.getDefaultVoice());
    }

    private boolean isLang(String normalizedLang, String shortCode, String longName) {
        return normalizedLang.equals(shortCode)
                || normalizedLang.startsWith(shortCode + "-")
                || normalizedLang.startsWith(shortCode + "_")
                || normalizedLang.equals(longName);
    }

    private String fallbackVoice(String value) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        if (ttsWorkerProperties.getFallbackVoice() != null && !ttsWorkerProperties.getFallbackVoice().isBlank()) {
            return ttsWorkerProperties.getFallbackVoice().trim();
        }
        return "en_US-lessac-medium";
    }

    private Double normalizeSpeed(Double value, double fallback) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return fallback;
        }
        if (value < 0.5d || value > 2.0d) {
            throw new BadRequestException("speed must be in range [0.5, 2.0]");
        }
        return value;
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available");
        }
    }
}
