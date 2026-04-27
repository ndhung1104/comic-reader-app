package com.group09.ComicReader.ai.controller;

import com.group09.ComicReader.ai.dto.AiSummaryRequest;
import com.group09.ComicReader.ai.dto.AiSummaryResponse;
import com.group09.ComicReader.common.entity.ModerationStatus;
import com.group09.ComicReader.ai.service.AiSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/summary")
public class AiSummaryController {

    private final AiSummaryService aiSummaryService;

    public AiSummaryController(AiSummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATOR')")
    public ResponseEntity<AiSummaryResponse> generateSummary(@RequestBody AiSummaryRequest request) {
        return ResponseEntity.ok(aiSummaryService.generateSummary(request.getComicId(), request.getChapterId()));
    }

    @PostMapping("/{id}/moderate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATOR')")
    public ResponseEntity<AiSummaryResponse> moderateSummary(
            @PathVariable Long id,
            @RequestParam ModerationStatus status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(aiSummaryService.moderateSummary(id, status, reason));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATOR')")
    public ResponseEntity<List<AiSummaryResponse>> getHistory(
            @RequestParam Long comicId,
            @RequestParam(required = false) Long chapterId) {
        return ResponseEntity.ok(aiSummaryService.getHistory(comicId, chapterId));
    }

    @GetMapping("/latest")
    public ResponseEntity<AiSummaryResponse> getLatestApproved(
            @RequestParam Long comicId,
            @RequestParam(required = false) Long chapterId) {
        return ResponseEntity.ok(aiSummaryService.getLatestApproved(comicId, chapterId));
    }
}
