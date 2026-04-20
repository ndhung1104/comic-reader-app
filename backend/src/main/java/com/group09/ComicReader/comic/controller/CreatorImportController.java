package com.group09.ComicReader.comic.controller;

import com.group09.ComicReader.comic.dto.ImportComicRequest;
import com.group09.ComicReader.importjob.dto.ImportJobResponse;
import com.group09.ComicReader.importjob.service.ImportJobService;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/creator/comics")
public class CreatorImportController {

    private final ImportJobService importJobService;
    private final UserRepository userRepository;

    public CreatorImportController(ImportJobService importJobService, UserRepository userRepository) {
        this.importJobService = importJobService;
        this.userRepository = userRepository;
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ImportJobResponse> importComic(@Valid @RequestBody ImportComicRequest request) {
        UserEntity user = getCurrentUser();
        ImportJobResponse response = importJobService.createJob(user, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
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
