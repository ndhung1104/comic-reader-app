package com.group09.ComicReader.importjob.controller;

import com.group09.ComicReader.importjob.dto.ImportJobResponse;
import com.group09.ComicReader.importjob.service.ImportJobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/creator/imports")
public class ImportJobController {

    private final ImportJobService importJobService;

    public ImportJobController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ImportJobResponse> getJob(@PathVariable Long jobId) {
        ImportJobResponse r = importJobService.getJob(jobId);
        return ResponseEntity.ok(r);
    }
}
