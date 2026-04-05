package com.group09.ComicReader.chapter.controller;

import com.group09.ComicReader.chapter.dto.ChapterPageResponse;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.service.ChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterResponse> getChapter(@PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getChapter(chapterId));
    }

    @GetMapping("/{chapterId}/pages")
    public ResponseEntity<List<ChapterPageResponse>> getChapterPages(@PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getPages(chapterId));
    }
}

