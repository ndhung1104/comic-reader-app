package com.group09.ComicReader.translationjob.controller;

import com.group09.ComicReader.translationjob.dto.CreateTranslationJobRequest;
import com.group09.ComicReader.translationjob.dto.TranslationJobArtifactResponse;
import com.group09.ComicReader.translationjob.dto.TranslationJobResponse;
import com.group09.ComicReader.translationjob.service.TranslationJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/translation-jobs")
public class TranslationJobController {

    private final TranslationJobService translationJobService;

    public TranslationJobController(TranslationJobService translationJobService) {
        this.translationJobService = translationJobService;
    }

    @PostMapping
    public ResponseEntity<TranslationJobResponse> createJob(@Valid @RequestBody CreateTranslationJobRequest request) {
        TranslationJobResponse response = translationJobService.createJob(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<TranslationJobResponse> getJob(@PathVariable Long jobId) {
        TranslationJobResponse response = translationJobService.getJob(jobId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{jobId}/artifacts")
    public ResponseEntity<TranslationJobArtifactResponse> getArtifacts(@PathVariable Long jobId) {
        TranslationJobArtifactResponse response = translationJobService.getArtifacts(jobId);
        return ResponseEntity.ok(response);
    }
}
