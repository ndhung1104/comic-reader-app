package com.group09.ComicReader.library.controller;

import com.group09.ComicReader.library.dto.FollowStatusResponse;
import com.group09.ComicReader.library.dto.FollowedComicResponse;
import com.group09.ComicReader.library.dto.ReadingHistoryRequest;
import com.group09.ComicReader.library.dto.RecentReadResponse;
import com.group09.ComicReader.library.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/followed")
    public ResponseEntity<List<FollowedComicResponse>> getFollowedComics() {
        return ResponseEntity.ok(libraryService.getFollowedComics());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentReadResponse>> getRecentReads() {
        return ResponseEntity.ok(libraryService.getRecentReads());
    }

    @GetMapping("/followed/{comicId}/status")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(@PathVariable Long comicId) {
        return ResponseEntity.ok(libraryService.getFollowStatus(comicId));
    }

    @PostMapping("/followed/{comicId}")
    public ResponseEntity<FollowStatusResponse> followComic(@PathVariable Long comicId) {
        return ResponseEntity.ok(libraryService.followComic(comicId));
    }

    @DeleteMapping("/followed/{comicId}")
    public ResponseEntity<FollowStatusResponse> unfollowComic(@PathVariable Long comicId) {
        return ResponseEntity.ok(libraryService.unfollowComic(comicId));
    }

    @PostMapping("/history")
    public ResponseEntity<RecentReadResponse> recordReadingHistory(@RequestBody ReadingHistoryRequest request) {
        return ResponseEntity.ok(libraryService.recordReadingHistory(request));
    }
}
