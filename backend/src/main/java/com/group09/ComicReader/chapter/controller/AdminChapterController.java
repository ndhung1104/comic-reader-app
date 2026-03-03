package com.group09.ComicReader.chapter.controller;

import com.group09.ComicReader.chapter.dto.ChapterPageResponse;
import com.group09.ComicReader.chapter.dto.ChapterRequest;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.service.ChapterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/chapters")
public class AdminChapterController {

    private final ChapterService chapterService;

    public AdminChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PutMapping("/{chapterId}")
    public ResponseEntity<ChapterResponse> updateChapter(@PathVariable Long chapterId,
                                                         @Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.ok(chapterService.updateChapter(chapterId, request));
    }

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long chapterId) {
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chapterId}/pages/upload")
    public ResponseEntity<List<ChapterPageResponse>> uploadPages(@PathVariable Long chapterId,
                                                                 @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(chapterService.uploadPages(chapterId, files));
    }
}

