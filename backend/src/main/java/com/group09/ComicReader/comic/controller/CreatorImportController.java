package com.group09.ComicReader.comic.controller;

import com.group09.ComicReader.comic.dto.ComicRequest;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.dto.ImportComicRequest;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.importjob.dto.ImportJobResponse;
import com.group09.ComicReader.importjob.service.ImportJobService;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creator/comics")
public class CreatorImportController {

    private final ImportJobService importJobService;
    private final ComicService comicService;
    private final UserRepository userRepository;

    public CreatorImportController(ImportJobService importJobService, ComicService comicService,
            UserRepository userRepository) {
        this.importJobService = importJobService;
        this.comicService = comicService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<java.util.Map<String, Object>> listMyComics(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserEntity user = getCurrentUser();
        Page<ComicResponse> results = comicService.listMyComics(user, PageRequest.of(page, size));
        return ResponseEntity.ok(java.util.Map.of(
                "items", results.getContent(),
                "page", results.getNumber(),
                "size", results.getSize(),
                "totalPages", results.getTotalPages(),
                "totalElements", results.getTotalElements()));
    }

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ComicResponse> createComic(@Valid @RequestBody ComicRequest request) {
        UserEntity user = getCurrentUser();
        ComicResponse response = comicService.creatorCreateComic(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ImportJobResponse> importComic(@Valid @RequestBody ImportComicRequest request) {
        UserEntity user = getCurrentUser();
        ImportJobResponse response = importJobService.createJob(user, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteComic(@PathVariable Long id) {
        UserEntity user = getCurrentUser();
        comicService.creatorDeleteComic(id, user);
        return ResponseEntity.noContent().build();
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
