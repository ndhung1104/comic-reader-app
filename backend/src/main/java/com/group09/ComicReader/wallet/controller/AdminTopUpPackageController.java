package com.group09.ComicReader.wallet.controller;

import com.group09.ComicReader.wallet.dto.TopUpPackageRequest;
import com.group09.ComicReader.wallet.dto.TopUpPackageResponse;
import com.group09.ComicReader.wallet.service.TopUpPackageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminTopUpPackageController {

    private final TopUpPackageService packageService;

    public AdminTopUpPackageController(TopUpPackageService packageService) {
        this.packageService = packageService;
    }

    /** Public endpoint — returns only active packages for wallet display */
    @GetMapping("/api/v1/packages")
    public ResponseEntity<List<TopUpPackageResponse>> getActivePackages() {
        return ResponseEntity.ok(packageService.getActivePackages());
    }

    /** Admin endpoint — returns all packages including disabled */
    @GetMapping("/api/v1/admin/packages")
    public ResponseEntity<List<TopUpPackageResponse>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @PostMapping("/api/v1/admin/packages")
    public ResponseEntity<TopUpPackageResponse> createPackage(@Valid @RequestBody TopUpPackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(packageService.createPackage(request));
    }

    @PutMapping("/api/v1/admin/packages/{id}")
    public ResponseEntity<TopUpPackageResponse> updatePackage(@PathVariable Long id,
                                                              @Valid @RequestBody TopUpPackageRequest request) {
        return ResponseEntity.ok(packageService.updatePackage(id, request));
    }

    @PutMapping("/api/v1/admin/packages/{id}/disable")
    public ResponseEntity<TopUpPackageResponse> disablePackage(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.disablePackage(id));
    }

    @PutMapping("/api/v1/admin/packages/{id}/enable")
    public ResponseEntity<TopUpPackageResponse> enablePackage(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.enablePackage(id));
    }
}
