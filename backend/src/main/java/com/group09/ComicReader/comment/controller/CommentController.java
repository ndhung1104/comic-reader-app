package com.group09.ComicReader.comment.controller;

import com.group09.ComicReader.comment.dto.CommentRequest;
import com.group09.ComicReader.comment.dto.CommentResponse;
import com.group09.ComicReader.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comics/{comicId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long comicId) {
        return ResponseEntity.ok(commentService.getCommentsByComic(comicId));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<CommentResponse>> getCommentsPaged(@PathVariable Long comicId,
            @RequestParam(name = "chapterId", required = false) Long chapterId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getCommentsByComicPaged(comicId, chapterId, pageable));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@PathVariable Long comicId,
                                                          @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(comicId, request));
    }
}
