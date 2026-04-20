package com.group09.ComicReader.importjob.service;

import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.comic.dto.ImportComicRequest;
import com.group09.ComicReader.comic.service.OTruyenService;
import com.group09.ComicReader.importjob.dto.ImportJobResponse;
import com.group09.ComicReader.importjob.entity.ImportJobEntity;
import com.group09.ComicReader.importjob.entity.ImportJobStatus;
import com.group09.ComicReader.importjob.repository.ImportJobRepository;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ImportJobService {

    private static final Logger log = LoggerFactory.getLogger(ImportJobService.class);

    private final ImportJobRepository importJobRepository;
    private final UserRepository userRepository;
    private final OTruyenService oTruyenService;
    private final ComicRepository comicRepository;

    public ImportJobService(ImportJobRepository importJobRepository,
            UserRepository userRepository,
            OTruyenService oTruyenService,
            ComicRepository comicRepository) {
        this.importJobRepository = importJobRepository;
        this.userRepository = userRepository;
        this.oTruyenService = oTruyenService;
        this.comicRepository = comicRepository;
    }

    @Transactional
    public ImportJobResponse createJob(UserEntity user, ImportComicRequest request) {
        ImportJobEntity job = new ImportJobEntity();
        job.setUser(user);
        job.setSource(request.getSourceType() == null ? "OTRUYEN" : request.getSourceType());
        job.setSourceUrl(request.getSourceUrl());
        job.setStatus(ImportJobStatus.QUEUED);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        ImportJobEntity saved = importJobRepository.save(job);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getJob(Long jobId) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Import job not found: " + jobId));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<ImportJobResponse> listMyJobs(UserEntity user, Pageable pageable) {
        return importJobRepository.findAllByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toResponse);
    }

    @Scheduled(fixedDelayString = "${import-job.sync-interval-ms:3000}")
    public void syncQueuedJobs() {
        List<ImportJobEntity> queued = importJobRepository.findTop50ByStatusInOrderByUpdatedAtAsc(
                List.of(ImportJobStatus.QUEUED));

        for (ImportJobEntity job : queued) {
            try {
                processJobAsync(job.getId());
            } catch (Exception e) {
                log.warn("Failed to submit import job for async processing (id={}): {}", job.getId(), e.getMessage());
            }
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processJobAsync(Long jobId) {
        Optional<ImportJobEntity> maybe = importJobRepository.findById(jobId);
        if (maybe.isEmpty())
            return;
        ImportJobEntity job = maybe.get();

        // avoid re-processing
        if (job.getStatus() != ImportJobStatus.QUEUED)
            return;

        job.setStatus(ImportJobStatus.RUNNING);
        job.setUpdatedAt(LocalDateTime.now());
        importJobRepository.save(job);

        try {
            ComicResponse resp = oTruyenService.importSingleComic(job.getSourceUrl());
            if (resp != null && resp.getId() != null) {
                job.setStatus(ImportJobStatus.SUCCEEDED);
                job.setUpdatedAt(LocalDateTime.now());
                // attach creator to comic
                comicRepository.findById(resp.getId()).ifPresent(c -> {
                    if (c.getSlug() == null || c.getSlug().isEmpty()) {
                        c.setSlug(com.group09.ComicReader.common.util.SlugUtils.toSlug(c.getTitle()) 
                            + "-" + System.currentTimeMillis());
                    }
                    c.setCreator(job.getUser());
                    comicRepository.save(c);
                    job.setResultComic(c);
                });
            } else {
                job.setStatus(ImportJobStatus.FAILED);
                job.setErrorMessage("Import returned no comic id");
                job.setUpdatedAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Import job failed (id={}): {}", job.getId(), e.getMessage());
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorMessage(truncateMessage(e.getMessage()));
            job.setUpdatedAt(LocalDateTime.now());
        } finally {
            importJobRepository.save(job);
        }
    }

    private String truncateMessage(String m) {
        if (m == null)
            return null;
        return m.length() <= 2000 ? m : m.substring(0, 2000);
    }

    private ImportJobResponse toResponse(ImportJobEntity job) {
        ImportJobResponse r = new ImportJobResponse();
        r.setId(job.getId());
        r.setSource(job.getSource());
        r.setSourceUrl(job.getSourceUrl());
        r.setStatus(job.getStatus());
        r.setErrorMessage(job.getErrorMessage());
        if (job.getResultComic() != null)
            r.setResultComicId(job.getResultComic().getId());
        r.setCreatedAt(job.getCreatedAt());
        r.setUpdatedAt(job.getUpdatedAt());
        return r;
    }
}
