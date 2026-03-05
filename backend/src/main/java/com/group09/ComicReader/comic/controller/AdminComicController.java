package com.group09.ComicReader.comic.controller;

import com.group09.ComicReader.chapter.dto.ChapterRequest;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.comic.dto.ComicRequest;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.comic.service.OTruyenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/comics")
public class AdminComicController {

    private final ComicService comicService;
    private final OTruyenService oTruyenService;

    public AdminComicController(ComicService comicService, OTruyenService oTruyenService) {
        this.comicService = comicService;
        this.oTruyenService = oTruyenService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> syncFromOTruyen() {
        oTruyenService.syncComicsFromOTruyen();
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<ComicResponse> createComic(@Valid @RequestBody ComicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comicService.createComic(request));
    }

    @PutMapping("/{comicId}")
    public ResponseEntity<ComicResponse> updateComic(@PathVariable Long comicId,
            @Valid @RequestBody ComicRequest request) {
        return ResponseEntity.ok(comicService.updateComic(comicId, request));
    }

    @DeleteMapping("/{comicId}")
    public ResponseEntity<Void> deleteComic(@PathVariable Long comicId) {
        comicService.deleteComic(comicId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{comicId}/chapters")
    public ResponseEntity<ChapterResponse> createChapter(@PathVariable Long comicId,
            @Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comicService.createChapter(comicId, request));
    }
}
