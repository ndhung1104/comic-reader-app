package com.group09.ComicReader.ai.entity;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.common.entity.ModerationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_summaries")
@Data
public class AiSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id", nullable = false)
    private ComicEntity comic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private ChapterEntity chapter;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationStatus status;

    @Column(name = "moderation_reason", columnDefinition = "TEXT")
    private String moderationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private UserEntity creator;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
