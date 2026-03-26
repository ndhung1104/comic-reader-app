package com.group09.ComicReader.comic.controller;

import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.service.ComicService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/comics")
public class ComicController {

    private final ComicService comicService;

    public ComicController(ComicService comicService) {
        this.comicService = comicService;
    }

    @GetMapping
    public ResponseEntity<Page<ComicResponse>> getComics(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        // If categoryId is "All", treat it as null so it doesn't filter
        if ("All".equalsIgnoreCase(categoryId)) {
            categoryId = null;
        }

        return ResponseEntity.ok(comicService.getComics(keyword, categoryId, pageable));
    }

    @GetMapping("/{comicId}")
    public ResponseEntity<ComicResponse> getComic(@PathVariable Long comicId) {
        return ResponseEntity.ok(comicService.getComic(comicId));
    }

    @GetMapping("/{comicId}/chapters")
    public ResponseEntity<List<ChapterResponse>> getComicChapters(@PathVariable Long comicId) {
        return ResponseEntity.ok(comicService.getChapters(comicId));
    }
}

