package com.group09.ComicReader.common.service;

import com.group09.ComicReader.common.entity.ModerationStatus;
import com.group09.ComicReader.importjob.entity.ImportJobEntity;
import com.group09.ComicReader.importjob.repository.ImportJobRepository;
import com.group09.ComicReader.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModerationService {

    private final ImportJobRepository importJobRepository;

    public ModerationService(ImportJobRepository importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    @Transactional
    public ImportJobEntity moderateImportJob(Long jobId, ModerationStatus status, String reason) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Import job not found: " + jobId));
        job.setModerationStatus(status);
        job.setModerationReason(reason);
        job.setUpdatedAt(LocalDateTime.now());
        return importJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<ImportJobEntity> getImportJobsForReview() {
        return importJobRepository.findByModerationStatus(ModerationStatus.REVIEW);
    }

    public com.group09.ComicReader.importjob.dto.ImportJobResponse toResponse(ImportJobEntity entity) {
        com.group09.ComicReader.importjob.dto.ImportJobResponse response = new com.group09.ComicReader.importjob.dto.ImportJobResponse();
        response.setId(entity.getId());
        response.setSource(entity.getSource());
        response.setSourceUrl(entity.getSourceUrl());
        response.setStatus(entity.getStatus());
        if (entity.getResultComic() != null) {
            response.setResultComicId(entity.getResultComic().getId());
        }
        response.setErrorMessage(entity.getErrorMessage());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setModerationStatus(entity.getModerationStatus());
        response.setModerationReason(entity.getModerationReason());
        return response;
    }
}
