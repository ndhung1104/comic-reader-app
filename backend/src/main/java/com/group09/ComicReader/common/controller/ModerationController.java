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
    private final com.group09.ComicReader.ai.service.AiSummaryService aiSummaryService;

    public ModerationController(ModerationService moderationService, 
                               AiSummaryRepository aiSummaryRepository,
                               com.group09.ComicReader.ai.service.AiSummaryService aiSummaryService) {
        this.moderationService = moderationService;
        this.aiSummaryRepository = aiSummaryRepository;
        this.aiSummaryService = aiSummaryService;
    }

    @GetMapping("/imports")
    public ResponseEntity<List<com.group09.ComicReader.importjob.dto.ImportJobResponse>> getPendingImports() {
        return ResponseEntity.ok(moderationService.getImportJobsForReview()
                .stream()
                .map(moderationService::toResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping("/imports/{id}")
    public ResponseEntity<com.group09.ComicReader.importjob.dto.ImportJobResponse> moderateImport(
            @PathVariable Long id,
            @RequestParam ModerationStatus status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(moderationService.toResponse(moderationService.moderateImportJob(id, status, reason)));
    }

    @GetMapping("/summaries")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AiSummaryResponse>> getPendingSummaries() {
        return ResponseEntity.ok(aiSummaryRepository.findByStatus(ModerationStatus.REVIEW)
                .stream()
                .map(aiSummaryService::toResponse)
                .collect(Collectors.toList()));
    }
}
