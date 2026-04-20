package com.group09.ComicReader.translationjob.service;

import com.group09.ComicReader.ai.entity.AiFeature;
import com.group09.ComicReader.ai.service.AiUsageService;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.chapter.service.ChapterService;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.common.exception.ServiceUnavailableException;
import com.group09.ComicReader.common.storage.FileStorageService;
import com.group09.ComicReader.config.properties.TranslationWorkerProperties;
import com.group09.ComicReader.config.properties.TtsWorkerProperties;
import com.group09.ComicReader.translationjob.client.TranslationWorkerClient;
import com.group09.ComicReader.translationjob.client.TtsWorkerClient;
import com.group09.ComicReader.translationjob.client.dto.WorkerJobStatusResponse;
import com.group09.ComicReader.translationjob.client.dto.WorkerOcrPageText;
import com.group09.ComicReader.translationjob.client.dto.WorkerPageInput;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobRequest;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobResponse;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerAudioPage;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerPageInput;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchRequest;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchResponse;
import com.group09.ComicReader.translationjob.dto.CreateTranslationJobRequest;
import com.group09.ComicReader.translationjob.dto.OcrPageTextResponse;
import com.group09.ComicReader.translationjob.dto.TranslationJobArtifactResponse;
import com.group09.ComicReader.translationjob.dto.TranslationJobResponse;
import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import com.group09.ComicReader.translationjob.entity.ChapterPageTtsAudioEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobStatus;
import com.group09.ComicReader.translationjob.repository.ChapterPageOcrTextRepository;
import com.group09.ComicReader.translationjob.repository.ChapterPageTtsAudioRepository;
import com.group09.ComicReader.translationjob.repository.TranslationJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Service
public class TranslationJobService {

    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationJobService.class);
    private static final List<TranslationJobStatus> ACTIVE_SYNC_STATUSES = List.of(
            TranslationJobStatus.QUEUED,
            TranslationJobStatus.RUNNING
    );

    private final TranslationJobRepository translationJobRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ChapterPageOcrTextRepository chapterPageOcrTextRepository;
    private final ChapterPageTtsAudioRepository chapterPageTtsAudioRepository;
    private final ChapterService chapterService;
    private final TranslationWorkerClient translationWorkerClient;
    private final TtsWorkerClient ttsWorkerClient;
    private final TranslationWorkerProperties translationWorkerProperties;
    private final TtsWorkerProperties ttsWorkerProperties;
    private final FileStorageService fileStorageService;
    private final AiUsageService aiUsageService;

    public TranslationJobService(TranslationJobRepository translationJobRepository,
            ChapterPageRepository chapterPageRepository,
            ChapterPageOcrTextRepository chapterPageOcrTextRepository,
            ChapterPageTtsAudioRepository chapterPageTtsAudioRepository,
            ChapterService chapterService,
            TranslationWorkerClient translationWorkerClient,
            TtsWorkerClient ttsWorkerClient,
            TranslationWorkerProperties translationWorkerProperties,
            TtsWorkerProperties ttsWorkerProperties,
            FileStorageService fileStorageService,
            AiUsageService aiUsageService) {
        this.translationJobRepository = translationJobRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.chapterPageOcrTextRepository = chapterPageOcrTextRepository;
        this.chapterPageTtsAudioRepository = chapterPageTtsAudioRepository;
        this.chapterService = chapterService;
        this.translationWorkerClient = translationWorkerClient;
        this.ttsWorkerClient = ttsWorkerClient;
        this.translationWorkerProperties = translationWorkerProperties;
        this.ttsWorkerProperties = ttsWorkerProperties;
        this.fileStorageService = fileStorageService;
        this.aiUsageService = aiUsageService;
    }

    @Transactional
    public TranslationJobResponse createJob(CreateTranslationJobRequest request) {
        ensureWorkerEnabled();

        AiUsageService.UsageContext usageContext = aiUsageService.beginUsage(
                AiFeature.OCR_TRANSLATION_JOB,
                "chapter-" + request.getChapterId(),
                null,
                "sourceLang=" + request.getSourceLang() + ",targetLang=" + request.getTargetLang()
        );

        try {
            ChapterEntity chapter = chapterService.getChapterEntity(request.getChapterId());
            List<ChapterPageEntity> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapter.getId());
            if (pages.isEmpty()) {
                throw new BadRequestException("Chapter has no pages to process");
            }

            String sourceLang = normalizeLanguage(request.getSourceLang(), "auto");
            String targetLang = normalizeLanguage(request.getTargetLang(), "vi");

            TranslationJobEntity job = new TranslationJobEntity();
            job.setChapter(chapter);
            job.setSourceLang(sourceLang);
            job.setTargetLang(targetLang);
            job.setStatus(TranslationJobStatus.QUEUED);
            job.setRequesterUserId(usageContext.userId());
            job.setCreatedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            job = translationJobRepository.save(job);

            WorkerSubmitJobRequest workerRequest = buildWorkerSubmitRequest(job, pages);
            WorkerSubmitJobResponse submitResponse;
            try {
                submitResponse = withRetry(() -> translationWorkerClient.submitJob(workerRequest));
            } catch (RuntimeException exception) {
                throw workerUnavailable(exception);
            }

            job.setExternalJobId(submitResponse.getJobId());
            job.setStatus(mapWorkerStatus(submitResponse.getStatus(), TranslationJobStatus.RUNNING));
            job.setErrorMessage(null);
            job.setUpdatedAt(LocalDateTime.now());
            if (isTerminal(job.getStatus())) {
                job.setCompletedAt(LocalDateTime.now());
            }

            TranslationJobEntity savedJob = translationJobRepository.save(job);
            aiUsageService.completeSuccess(
                    usageContext,
                    "translation-worker",
                    null,
                    pages.size(),
                    "jobId=" + savedJob.getId() + ",externalJobId=" + savedJob.getExternalJobId()
            );
            return toResponse(savedJob);
        } catch (RuntimeException exception) {
            aiUsageService.completeFailure(usageContext, "translation-worker", null, exception.getMessage());
            throw exception;
        }
    }

    @Transactional
    public TranslationJobResponse getJob(Long jobId) {
        ensureWorkerEnabled();

        TranslationJobEntity job = translationJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Translation job not found: " + jobId));

        refreshJobFromWorker(job);
        return toResponse(job);
    }

    @Transactional
    public TranslationJobArtifactResponse getArtifacts(Long jobId) {
        ensureWorkerEnabled();

        TranslationJobEntity job = translationJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Translation job not found: " + jobId));

        refreshJobFromWorker(job);

        if (job.getExternalJobId() == null || job.getExternalJobId().isBlank()) {
            throw new BadRequestException("Translation job does not have an external worker id yet");
        }

        WorkerJobStatusResponse artifactsResponse;
        try {
            artifactsResponse = withRetry(
                    () -> translationWorkerClient.fetchArtifacts(job.getExternalJobId())
            );
        } catch (RuntimeException exception) {
            throw workerUnavailable(exception);
        }

        persistOcrPages(job, artifactsResponse.getOcrPages());

        TranslationJobArtifactResponse response = new TranslationJobArtifactResponse();
        response.setJobId(job.getId());
        response.setExternalJobId(job.getExternalJobId());
        response.setStatus(job.getStatus());
        response.setArtifacts(artifactsResponse.getArtifacts());
        response.setOcrPages(toOcrPageResponses(artifactsResponse.getOcrPages()));
        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ChapterPageOcrTextEntity> ensureChapterOcrText(Long chapterId, String sourceLang) {
        ensureWorkerEnabled();

        ChapterEntity chapter = chapterService.getChapterEntity(chapterId);
        List<ChapterPageEntity> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
        if (pages.isEmpty()) {
            throw new BadRequestException("Chapter has no pages to process");
        }

        String normalizedSource = normalizeLanguage(sourceLang, "auto");
        List<ChapterPageOcrTextEntity> existing = chapterPageOcrTextRepository
                .findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, normalizedSource);
        if (existing.size() >= pages.size()) {
            return existing;
        }

        Optional<TranslationJobEntity> activeJob = translationJobRepository
                .findFirstByChapterIdAndSourceLangAndStatusInOrderByCreatedAtDesc(
                        chapterId,
                        normalizedSource,
                        ACTIVE_SYNC_STATUSES
                );

        if (activeJob.isEmpty()) {
            CreateTranslationJobRequest request = new CreateTranslationJobRequest();
            request.setChapterId(chapter.getId());
            request.setSourceLang(normalizedSource);
            request.setTargetLang("vi");
            TranslationJobResponse created = createJob(request);
            activeJob = translationJobRepository.findById(created.getId());
        }

        activeJob.ifPresent(this::refreshJobFromWorkerSafely);
        return chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, normalizedSource);
    }

    @Transactional(readOnly = true)
    public List<ChapterPageOcrTextEntity> getChapterOcrText(Long chapterId, String sourceLang) {
        return chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(
                chapterId,
                normalizeLanguage(sourceLang, "auto")
        );
    }

    @Scheduled(fixedDelayString = "${translation-worker.sync-interval-ms:3000}")
    public void syncActiveJobs() {
        if (!translationWorkerProperties.isEnabled()) {
            return;
        }

        List<TranslationJobEntity> activeJobs = translationJobRepository
                .findTop50ByStatusInOrderByUpdatedAtAsc(ACTIVE_SYNC_STATUSES);
        for (TranslationJobEntity activeJob : activeJobs) {
            refreshJobFromWorkerSafely(activeJob);
        }
    }

    private void refreshJobFromWorkerSafely(TranslationJobEntity job) {
        try {
            refreshJobFromWorker(job);
        } catch (ServiceUnavailableException exception) {
            LOGGER.debug("Skip translation job sync due to worker unavailable (jobId={})", job.getId());
        } catch (RuntimeException exception) {
            LOGGER.warn("Unexpected translation job sync error (jobId={}): {}", job.getId(), exception.getMessage());
        }
    }

    private void refreshJobFromWorker(TranslationJobEntity job) {
        if (job.getExternalJobId() == null || job.getExternalJobId().isBlank()) {
            return;
        }

        if (isTerminal(job.getStatus()) && job.isOcrPersisted()) {
            return;
        }

        WorkerJobStatusResponse statusResponse;
        try {
            statusResponse = withRetry(
                    () -> translationWorkerClient.fetchJobStatus(job.getExternalJobId())
            );
        } catch (HttpClientErrorException.NotFound exception) {
            markJobFailed(job, "OCR worker job was not found.");
            return;
        } catch (RuntimeException exception) {
            throw workerUnavailable(exception);
        }

        TranslationJobStatus newStatus = mapWorkerStatus(statusResponse.getStatus(), job.getStatus());
        job.setStatus(newStatus);
        job.setErrorMessage(truncateMessage(statusResponse.getError()));
        job.setUpdatedAt(LocalDateTime.now());

        if (isTerminal(newStatus) && job.getCompletedAt() == null) {
            job.setCompletedAt(LocalDateTime.now());
        }

        persistOcrPages(job, statusResponse.getOcrPages());
        translationJobRepository.save(job);
    }

    private void markJobFailed(TranslationJobEntity job, String errorMessage) {
        job.setStatus(TranslationJobStatus.FAILED);
        job.setErrorMessage(truncateMessage(errorMessage));
        job.setUpdatedAt(LocalDateTime.now());
        if (job.getCompletedAt() == null) {
            job.setCompletedAt(LocalDateTime.now());
        }
        translationJobRepository.save(job);
    }

    private void persistOcrPages(TranslationJobEntity job, List<WorkerOcrPageText> ocrPages) {
        if (ocrPages == null || ocrPages.isEmpty()) {
            return;
        }

        boolean persistedAny = false;

        for (WorkerOcrPageText workerPage : ocrPages) {
            if (workerPage == null || workerPage.getPageNumber() == null || workerPage.getPageNumber() <= 0) {
                continue;
            }

            Long chapterId = workerPage.getChapterId() == null ? job.getChapter().getId() : workerPage.getChapterId();
            if (!job.getChapter().getId().equals(chapterId)) {
                continue;
            }

            String sourceLang = normalizeLanguage(workerPage.getSourceLang(), job.getSourceLang());
            Optional<ChapterPageOcrTextEntity> existing = chapterPageOcrTextRepository
                    .findByChapterIdAndPageNumberAndSourceLang(chapterId, workerPage.getPageNumber(), sourceLang);

            ChapterPageOcrTextEntity entity = existing.orElseGet(() -> {
                ChapterPageOcrTextEntity created = new ChapterPageOcrTextEntity();
                created.setChapter(job.getChapter());
                created.setPageNumber(workerPage.getPageNumber());
                created.setSourceLang(sourceLang);
                created.setCreatedAt(LocalDateTime.now());
                return created;
            });

            entity.setOcrText(workerPage.getOcrText() == null ? "" : workerPage.getOcrText());
            entity.setOcrJob(job);
            entity.setUpdatedAt(LocalDateTime.now());
            ChapterPageOcrTextEntity savedOcr = chapterPageOcrTextRepository.save(entity);
            maybeGeneratePageAudio(savedOcr);
            persistedAny = true;
        }

        if (persistedAny) {
            job.setOcrPersisted(true);
            job.setUpdatedAt(LocalDateTime.now());
            if (job.getStatus() == TranslationJobStatus.SUCCEEDED && job.getCompletedAt() == null) {
                job.setCompletedAt(LocalDateTime.now());
            }
        }
    }

    private WorkerSubmitJobRequest buildWorkerSubmitRequest(TranslationJobEntity job, List<ChapterPageEntity> pages) {
        WorkerSubmitJobRequest request = new WorkerSubmitJobRequest();
        request.setIdempotencyKey("translation-job-" + job.getId());
        request.setChapterId(job.getChapter().getId());
        request.setComicId(job.getChapter().getComic().getId());
        request.setSourceLang(job.getSourceLang());
        request.setTargetLang(job.getTargetLang());

        List<WorkerPageInput> workerPages = new ArrayList<>();
        for (int index = 0; index < pages.size(); index++) {
            ChapterPageEntity page = pages.get(index);
            WorkerPageInput workerPageInput = new WorkerPageInput();
            // Normalize page number to 1-based sequence for worker compatibility.
            workerPageInput.setPageNumber(index + 1);
            workerPageInput.setImageUrl(page.getImageUrl());
            workerPages.add(workerPageInput);
        }
        request.setPages(workerPages);
        return request;
    }

    private TranslationJobResponse toResponse(TranslationJobEntity entity) {
        TranslationJobResponse response = new TranslationJobResponse();
        response.setId(entity.getId());
        response.setChapterId(entity.getChapter().getId());
        response.setExternalJobId(entity.getExternalJobId());
        response.setStatus(entity.getStatus());
        response.setSourceLang(entity.getSourceLang());
        response.setTargetLang(entity.getTargetLang());
        response.setErrorMessage(entity.getErrorMessage());
        response.setOcrPersisted(entity.isOcrPersisted());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }

    private List<OcrPageTextResponse> toOcrPageResponses(List<WorkerOcrPageText> ocrPages) {
        List<OcrPageTextResponse> responses = new ArrayList<>();
        if (ocrPages == null) {
            return responses;
        }

        for (WorkerOcrPageText page : ocrPages) {
            if (page == null) {
                continue;
            }
            OcrPageTextResponse response = new OcrPageTextResponse();
            response.setChapterId(page.getChapterId());
            response.setPageNumber(page.getPageNumber());
            response.setSourceLang(page.getSourceLang());
            response.setOcrText(page.getOcrText());
            responses.add(response);
        }
        return responses;
    }

    private TranslationJobStatus mapWorkerStatus(String workerStatus, TranslationJobStatus defaultStatus) {
        if (workerStatus == null || workerStatus.isBlank()) {
            return defaultStatus;
        }

        String normalized = workerStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "QUEUED" -> TranslationJobStatus.QUEUED;
            case "RUNNING", "IN_PROGRESS" -> TranslationJobStatus.RUNNING;
            case "SUCCEEDED", "SUCCESS", "COMPLETED", "DONE" -> TranslationJobStatus.SUCCEEDED;
            case "FAILED", "ERROR" -> TranslationJobStatus.FAILED;
            case "CANCELLED", "CANCELED" -> TranslationJobStatus.CANCELLED;
            default -> defaultStatus;
        };
    }

    private boolean isTerminal(TranslationJobStatus status) {
        return status == TranslationJobStatus.SUCCEEDED
                || status == TranslationJobStatus.FAILED
                || status == TranslationJobStatus.CANCELLED;
    }

    private String normalizeLanguage(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim();
        if (normalized.length() <= 1000) {
            return normalized;
        }
        return normalized.substring(0, 1000);
    }

    private <T> T withRetry(Supplier<T> supplier) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException exception) {
                lastException = exception;
            }
        }

        throw lastException == null ? new BadRequestException("Unknown worker error") : lastException;
    }

    private void ensureWorkerEnabled() {
        if (!translationWorkerProperties.isEnabled()) {
            throw new ServiceUnavailableException("OCR service is temporarily unavailable.");
        }
    }

    private ServiceUnavailableException workerUnavailable(RuntimeException exception) {
        return new ServiceUnavailableException("OCR service is temporarily unavailable.");
    }

    private void maybeGeneratePageAudio(ChapterPageOcrTextEntity ocrPage) {
        if (!ttsWorkerProperties.isEnabled()) {
            return;
        }
        if (ocrPage == null || ocrPage.getPageNumber() == null || ocrPage.getPageNumber() <= 0) {
            return;
        }
        if (ocrPage.getOcrText() == null || ocrPage.getOcrText().isBlank()) {
            return;
        }

        String lang = normalizeLanguage(ocrPage.getSourceLang(), normalizeLanguage(ttsWorkerProperties.getDefaultLang(), "auto"))
                .toLowerCase(Locale.ROOT);
        String voice = resolveVoice(null, lang);
        Double speed = ttsWorkerProperties.getDefaultSpeed();
        String textHash = hashText(ocrPage.getOcrText());
        Long chapterId = ocrPage.getChapter().getId();
        Integer pageNumber = ocrPage.getPageNumber();

        Optional<ChapterPageTtsAudioEntity> existingOpt = chapterPageTtsAudioRepository
                .findByChapterIdAndPageNumberAndLangAndVoiceAndSpeed(chapterId, pageNumber, lang, voice, speed);
        if (existingOpt.isPresent()) {
            ChapterPageTtsAudioEntity existing = existingOpt.get();
            if (existing.getAudioPath() != null
                    && !existing.getAudioPath().isBlank()
                    && textHash.equals(existing.getSourceTextHash())) {
                return;
            }
        }

        TtsWorkerSynthesizeBatchRequest request = new TtsWorkerSynthesizeBatchRequest();
        request.setChapterId(String.valueOf(chapterId));
        request.setLang(lang);
        request.setVoice(voice);
        request.setSpeed(speed);
        TtsWorkerPageInput pageInput = new TtsWorkerPageInput();
        pageInput.setPageNumber(pageNumber);
        pageInput.setText(ocrPage.getOcrText());
        request.setPages(List.of(pageInput));

        try {
            TtsWorkerSynthesizeBatchResponse response = ttsWorkerClient.synthesizeBatch(request);
            if (!"SUCCEEDED".equalsIgnoreCase(response.getStatus())
                    || response.getAudioPages() == null
                    || response.getAudioPages().isEmpty()) {
                return;
            }

            TtsWorkerAudioPage generated = response.getAudioPages().get(0);
            if (generated.getAudioBase64() == null || generated.getAudioBase64().isBlank()) {
                return;
            }

            byte[] audioBytes = Base64.getDecoder().decode(generated.getAudioBase64());
            String audioPath = fileStorageService.storeChapterPageAudio(
                    chapterId,
                    pageNumber,
                    lang,
                    voice,
                    speed,
                    audioBytes
            );

            ChapterPageTtsAudioEntity target = existingOpt.orElseGet(() -> {
                ChapterPageTtsAudioEntity created = new ChapterPageTtsAudioEntity();
                created.setChapter(ocrPage.getChapter());
                created.setPageNumber(pageNumber);
                created.setLang(lang);
                created.setVoice(voice);
                created.setSpeed(speed);
                created.setCreatedAt(LocalDateTime.now());
                return created;
            });
            target.setAudioPath(audioPath);
            target.setDurationMs(generated.getDurationMs());
            target.setSourceTextHash(textHash);
            target.setSourceOcrJob(ocrPage.getOcrJob());
            target.setUpdatedAt(LocalDateTime.now());
            chapterPageTtsAudioRepository.save(target);
            LOGGER.info("Auto TTS generated for chapter={} page={} lang={} voice={}", chapterId, pageNumber, lang, voice);
        } catch (RuntimeException exception) {
            LOGGER.debug(
                    "Skip auto TTS for chapter={} page={} due to worker/storage error: {}",
                    chapterId,
                    pageNumber,
                    exception.getMessage()
            );
        }
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
