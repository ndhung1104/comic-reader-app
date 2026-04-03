package com.group09.ComicReader.comic.controller;

import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.dto.ImportComicRequest;
import com.group09.ComicReader.comic.service.OTruyenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/comics")
public class AdminImportController {

    private final OTruyenService oTruyenService;

    public AdminImportController(OTruyenService oTruyenService) {
        this.oTruyenService = oTruyenService;
    }

    @PostMapping("/import")
    public ResponseEntity<ComicResponse> importComic(@Valid @RequestBody ImportComicRequest request) {
        ComicResponse response = oTruyenService.importSingleComic(request.getSourceUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
