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

    public List<ImportJobEntity> getImportJobsForReview() {
        return importJobRepository.findByModerationStatus(ModerationStatus.REVIEW);
    }
}
