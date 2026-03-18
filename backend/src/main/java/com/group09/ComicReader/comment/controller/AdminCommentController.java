package com.group09.ComicReader.comment.controller;

import com.group09.ComicReader.comment.dto.CommentResponse;
import com.group09.ComicReader.comment.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/comics/{comicId}/comments")
    public ResponseEntity<Page<CommentResponse>> getAllComments(@PathVariable Long comicId,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getAllCommentsByComic(comicId, pageable));
    }

    @PutMapping("/comments/{commentId}/hide")
    public ResponseEntity<CommentResponse> hideComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.hideComment(commentId));
    }

    @PutMapping("/comments/{commentId}/unhide")
    public ResponseEntity<CommentResponse> unhideComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.unhideComment(commentId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
