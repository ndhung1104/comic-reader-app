package com.group09.ComicReader.comic.controller;

import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.dto.RatingRequest;
import com.group09.ComicReader.comic.service.ComicService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

        if ("All".equalsIgnoreCase(categoryId)) {
            categoryId = null;
        }
        return ResponseEntity.ok(comicService.getComics(keyword, categoryId, pageable));
    }

    @GetMapping("/trending")
    public ResponseEntity<Page<ComicResponse>> getTrending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comicService.getTrendingComics(pageable));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<Page<ComicResponse>> getTopRated(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comicService.getTopRatedComics(pageable));
    }

    @GetMapping("/{comicId}")
    public ResponseEntity<ComicResponse> getComic(@PathVariable Long comicId) {
        return ResponseEntity.ok(comicService.getComic(comicId));
    }

    @GetMapping("/{comicId}/chapters")
    public ResponseEntity<List<ChapterResponse>> getComicChapters(@PathVariable Long comicId) {
        return ResponseEntity.ok(comicService.getChapters(comicId));
    }

    @GetMapping("/{comicId}/related")
    public ResponseEntity<List<ComicResponse>> getRelatedComics(
            @PathVariable Long comicId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(comicService.getRelatedComics(comicId, size));
    }

    @PostMapping("/{comicId}/rate")
    public ResponseEntity<Map<String, String>> rateComic(
            @PathVariable Long comicId,
            @Valid @RequestBody RatingRequest request) {
        comicService.rateComic(comicId, request.getScore());
        return ResponseEntity.ok(Map.of("message", "Rating saved"));
    }

    @PostMapping("/{comicId}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long comicId) {
        comicService.incrementViewCount(comicId);
        return ResponseEntity.ok().build();
    }
}
