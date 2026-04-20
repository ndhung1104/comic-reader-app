package com.group09.ComicReader.creatorrequest.controller;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.creatorrequest.dto.CreateCreatorRequest;
import com.group09.ComicReader.creatorrequest.dto.CreatorRequestResponse;
import com.group09.ComicReader.creatorrequest.entity.CreatorRequestEntity;
import com.group09.ComicReader.creatorrequest.service.CreatorRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
public class CreatorRequestController {

    private final CreatorRequestService service;
    private final UserRepository userRepository;

    public CreatorRequestController(CreatorRequestService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping("/api/v1/creator-requests")
    public ResponseEntity<CreatorRequestResponse> create(@Valid @RequestBody CreateCreatorRequest request) {
        UserEntity user = getCurrentUser();
        CreatorRequestEntity created = service.createRequest(user, request.getMessage());
        return ResponseEntity.ok(toResponse(created));
    }

    @GetMapping("/api/v1/creator-requests/my")
    public ResponseEntity<CreatorRequestResponse> getMyRequest() {
        UserEntity user = getCurrentUser();
        return service.getLatestRequestByUser(user)
                .map(req -> ResponseEntity.ok(toResponse(req)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/creator-requests")
    public ResponseEntity<java.util.Map<String, Object>> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CreatorRequestEntity> results = service.listRequests(PageRequest.of(page, size));
        java.util.List<CreatorRequestResponse> items = results.stream().map(this::toResponse)
                .collect(Collectors.toList());
        java.util.Map<String, Object> payload = java.util.Map.of(
                "items", items,
                "page", results.getNumber(),
                "size", results.getSize(),
                "totalPages", results.getTotalPages(),
                "totalElements", results.getTotalElements());
        return ResponseEntity.ok(payload);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/creator-requests/{id}/approve")
    public ResponseEntity<CreatorRequestResponse> approve(@PathVariable Long id,
            @RequestParam(required = false) String adminMessage) {
        UserEntity admin = getCurrentUser();
        CreatorRequestEntity updated = service.approveRequest(id, admin, adminMessage);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/creator-requests/{id}/deny")
    public ResponseEntity<CreatorRequestResponse> deny(@PathVariable Long id,
            @RequestParam(required = false) String adminMessage) {
        UserEntity admin = getCurrentUser();
        CreatorRequestEntity updated = service.denyRequest(id, admin, adminMessage);
        return ResponseEntity.ok(toResponse(updated));
    }

    private CreatorRequestResponse toResponse(CreatorRequestEntity e) {
        CreatorRequestResponse r = new CreatorRequestResponse();
        r.setId(e.getId());
        if (e.getUser() != null) {
            r.setUserId(e.getUser().getId());
            r.setUserEmail(e.getUser().getEmail());
        }
        r.setMessage(e.getMessage());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setProcessedAt(e.getProcessedAt());
        if (e.getProcessedBy() != null) {
            r.setProcessedById(e.getProcessedBy().getId());
            r.setProcessedByEmail(e.getProcessedBy().getEmail());
        }
        r.setAdminMessage(e.getAdminMessage());
        return r;
    }

    private UserEntity getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
