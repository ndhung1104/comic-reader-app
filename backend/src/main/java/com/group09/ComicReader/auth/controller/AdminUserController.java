package com.group09.ComicReader.auth.controller;

import com.group09.ComicReader.auth.dto.UserResponse;
import com.group09.ComicReader.auth.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminUserService.getUsers(pageable));
    }

    @PutMapping("/{userId}/ban")
    public ResponseEntity<UserResponse> banUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.banUser(userId));
    }

    @PutMapping("/{userId}/unban")
    public ResponseEntity<UserResponse> unbanUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.unbanUser(userId));
    }
}
