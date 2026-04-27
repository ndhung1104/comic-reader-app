package com.group09.ComicReader.common.controller;

import com.group09.ComicReader.ai.dto.AiSummaryResponse;
import com.group09.ComicReader.ai.entity.AiSummaryEntity;
import com.group09.ComicReader.ai.repository.AiSummaryRepository;
import com.group09.ComicReader.common.entity.ModerationStatus;
import com.group09.ComicReader.common.service.ModerationService;
import com.group09.ComicReader.importjob.entity.ImportJobEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
public class ModerationController {

    private final ModerationService moderationService;
    private final AiSummaryRepository aiSummaryRepository;

    public ModerationController(ModerationService moderationService, AiSummaryRepository aiSummaryRepository) {
        this.moderationService = moderationService;
        this.aiSummaryRepository = aiSummaryRepository;
    }

    @GetMapping("/imports")
    public ResponseEntity<List<ImportJobEntity>> getPendingImports() {
        return ResponseEntity.ok(moderationService.getImportJobsForReview());
    }

    @PostMapping("/imports/{id}")
    public ResponseEntity<ImportJobEntity> moderateImport(
            @PathVariable Long id,
            @RequestParam ModerationStatus status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(moderationService.moderateImportJob(id, status, reason));
    }

    @GetMapping("/summaries")
    public ResponseEntity<List<AiSummaryEntity>> getPendingSummaries() {
        return ResponseEntity.ok(aiSummaryRepository.findByStatus(ModerationStatus.REVIEW));
    }
}
