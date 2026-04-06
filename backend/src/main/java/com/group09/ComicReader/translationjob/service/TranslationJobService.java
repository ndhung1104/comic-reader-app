package com.group09.ComicReader.translationjob.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.chapter.service.ChapterService;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.translationjob.client.TranslationWorkerClient;
import com.group09.ComicReader.translationjob.client.dto.WorkerJobStatusResponse;
import com.group09.ComicReader.translationjob.client.dto.WorkerOcrPageText;
import com.group09.ComicReader.translationjob.client.dto.WorkerPageInput;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobRequest;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobResponse;
import com.group09.ComicReader.translationjob.dto.CreateTranslationJobRequest;
import com.group09.ComicReader.translationjob.dto.OcrPageTextResponse;
import com.group09.ComicReader.translationjob.dto.TranslationJobArtifactResponse;
import com.group09.ComicReader.translationjob.dto.TranslationJobResponse;
import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobStatus;
import com.group09.ComicReader.translationjob.repository.ChapterPageOcrTextRepository;
import com.group09.ComicReader.translationjob.repository.TranslationJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class TranslationJobService {

    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final TranslationJobRepository translationJobRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ChapterPageOcrTextRepository chapterPageOcrTextRepository;
    private final ChapterService chapterService;
    private final TranslationWorkerClient translationWorkerClient;

    public TranslationJobService(TranslationJobRepository translationJobRepository,
            ChapterPageRepository chapterPageRepository,
            ChapterPageOcrTextRepository chapterPageOcrTextRepository,
            ChapterService chapterService,
            TranslationWorkerClient translationWorkerClient) {
        this.translationJobRepository = translationJobRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.chapterPageOcrTextRepository = chapterPageOcrTextRepository;
        this.chapterService = chapterService;
        this.translationWorkerClient = translationWorkerClient;
    }

    @Transactional
    public TranslationJobResponse createJob(CreateTranslationJobRequest request) {
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
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        job = translationJobRepository.save(job);

        WorkerSubmitJobRequest workerRequest = buildWorkerSubmitRequest(job, pages);

        try {
            WorkerSubmitJobResponse submitResponse = withRetry(() -> translationWorkerClient.submitJob(workerRequest));
            job.setExternalJobId(submitResponse.getJobId());
            job.setStatus(mapWorkerStatus(submitResponse.getStatus(), TranslationJobStatus.RUNNING));
            job.setErrorMessage(null);
            job.setUpdatedAt(LocalDateTime.now());
            if (isTerminal(job.getStatus())) {
                job.setCompletedAt(LocalDateTime.now());
            }
        } catch (Exception exception) {
            job.setStatus(TranslationJobStatus.FAILED);
            job.setErrorMessage(truncateMessage(exception.getMessage()));
            job.setUpdatedAt(LocalDateTime.now());
            job.setCompletedAt(LocalDateTime.now());
        }

        return toResponse(translationJobRepository.save(job));
    }

    @Transactional
    public TranslationJobResponse getJob(Long jobId) {
        TranslationJobEntity job = translationJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Translation job not found: " + jobId));

        refreshJobFromWorker(job);
        return toResponse(job);
    }

    @Transactional
    public TranslationJobArtifactResponse getArtifacts(Long jobId) {
        TranslationJobEntity job = translationJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Translation job not found: " + jobId));

        refreshJobFromWorker(job);

        if (job.getExternalJobId() == null || job.getExternalJobId().isBlank()) {
            throw new BadRequestException("Translation job does not have an external worker id yet");
        }

        WorkerJobStatusResponse artifactsResponse = withRetry(
                () -> translationWorkerClient.fetchArtifacts(job.getExternalJobId())
        );

        persistOcrPages(job, artifactsResponse.getOcrPages());

        TranslationJobArtifactResponse response = new TranslationJobArtifactResponse();
        response.setJobId(job.getId());
        response.setExternalJobId(job.getExternalJobId());
        response.setStatus(job.getStatus());
        response.setArtifacts(artifactsResponse.getArtifacts());
        response.setOcrPages(toOcrPageResponses(artifactsResponse.getOcrPages()));
        return response;
    }

    private void refreshJobFromWorker(TranslationJobEntity job) {
        if (job.getExternalJobId() == null || job.getExternalJobId().isBlank()) {
            return;
        }

        if (isTerminal(job.getStatus()) && job.isOcrPersisted()) {
            return;
        }

        try {
            WorkerJobStatusResponse statusResponse = withRetry(
                    () -> translationWorkerClient.fetchJobStatus(job.getExternalJobId())
            );

            TranslationJobStatus newStatus = mapWorkerStatus(statusResponse.getStatus(), job.getStatus());
            job.setStatus(newStatus);
            job.setErrorMessage(truncateMessage(statusResponse.getError()));
            job.setUpdatedAt(LocalDateTime.now());

            if (isTerminal(newStatus) && job.getCompletedAt() == null) {
                job.setCompletedAt(LocalDateTime.now());
            }

            persistOcrPages(job, statusResponse.getOcrPages());
            translationJobRepository.save(job);
        } catch (Exception exception) {
            job.setUpdatedAt(LocalDateTime.now());
            if (job.getErrorMessage() == null || job.getErrorMessage().isBlank()) {
                job.setErrorMessage(truncateMessage("Worker sync failed: " + exception.getMessage()));
            }
            translationJobRepository.save(job);
        }
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
            chapterPageOcrTextRepository.save(entity);
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
}
