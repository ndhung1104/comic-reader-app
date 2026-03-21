package com.group09.ComicReader.comment.entity;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.comic.entity.ComicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@ToString(exclude = {"comic", "user"})
@EqualsAndHashCode(exclude = {"comic", "user"})
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id", nullable = false)
    private ComicEntity comic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private ChapterEntity chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentEntity parentComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_comment_id")
    private CommentEntity rootComment;

    @Column(nullable = false)
    private int depth = 0;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "source_type", nullable = false)
    private String sourceType = "NORMAL";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
