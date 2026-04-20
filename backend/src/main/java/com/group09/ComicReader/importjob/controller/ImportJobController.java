package com.group09.ComicReader.importjob.controller;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.importjob.dto.ImportJobResponse;
import com.group09.ComicReader.importjob.service.ImportJobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creator/imports")
public class ImportJobController {

    private final ImportJobService importJobService;
    private final UserRepository userRepository;

    public ImportJobController(ImportJobService importJobService, UserRepository userRepository) {
        this.importJobService = importJobService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<java.util.Map<String, Object>> listMyJobs(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserEntity user = getCurrentUser();
        Page<ImportJobResponse> results = importJobService.listMyJobs(user, PageRequest.of(page, size));
        return ResponseEntity.ok(java.util.Map.of(
                "items", results.getContent(),
                "page", results.getNumber(),
                "size", results.getSize(),
                "totalPages", results.getTotalPages(),
                "totalElements", results.getTotalElements()));
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ImportJobResponse> getJob(@PathVariable Long jobId) {
        ImportJobResponse r = importJobService.getJob(jobId);
        return ResponseEntity.ok(r);
    }

    private UserEntity getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email).orElse(null);
    }
}
