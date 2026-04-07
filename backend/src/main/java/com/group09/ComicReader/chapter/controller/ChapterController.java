package com.group09.ComicReader.chapter.controller;

import com.group09.ComicReader.chapter.dto.ChapterAudioPlaylistRequest;
import com.group09.ComicReader.chapter.dto.ChapterAudioPlaylistResponse;
import com.group09.ComicReader.chapter.dto.ChapterPageResponse;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.service.ChapterAudioPlaylistService;
import com.group09.ComicReader.chapter.service.ChapterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chapters")
public class ChapterController {

    private final ChapterService chapterService;
    private final ChapterAudioPlaylistService chapterAudioPlaylistService;

    public ChapterController(ChapterService chapterService, ChapterAudioPlaylistService chapterAudioPlaylistService) {
        this.chapterService = chapterService;
        this.chapterAudioPlaylistService = chapterAudioPlaylistService;
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterResponse> getChapter(@PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getChapter(chapterId));
    }

    @GetMapping("/{chapterId}/pages")
    public ResponseEntity<List<ChapterPageResponse>> getChapterPages(@PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getPages(chapterId));
    }

    @PostMapping("/{chapterId}/audio-playlist")
    public ResponseEntity<ChapterAudioPlaylistResponse> createOrGetAudioPlaylist(@PathVariable Long chapterId,
            @Valid @RequestBody(required = false) ChapterAudioPlaylistRequest request) {
        return ResponseEntity.ok(chapterAudioPlaylistService.createOrGetPlaylist(chapterId, request));
    }

    @GetMapping("/{chapterId}/audio-playlist")
    public ResponseEntity<ChapterAudioPlaylistResponse> getAudioPlaylist(@PathVariable Long chapterId,
            ChapterAudioPlaylistRequest request) {
        return ResponseEntity.ok(chapterAudioPlaylistService.getExistingPlaylist(chapterId, request));
    }
}

