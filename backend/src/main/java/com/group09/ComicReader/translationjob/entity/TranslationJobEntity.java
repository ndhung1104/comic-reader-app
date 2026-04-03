package com.group09.ComicReader.translationjob.entity;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_jobs")
@Data
@ToString(exclude = {"chapter"})
@EqualsAndHashCode(exclude = {"chapter"})
public class TranslationJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private ChapterEntity chapter;

    @Column(name = "external_job_id")
    private String externalJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranslationJobStatus status = TranslationJobStatus.QUEUED;

    @Column(name = "source_lang", nullable = false)
    private String sourceLang;

    @Column(name = "target_lang")
    private String targetLang;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ocr_persisted", nullable = false)
    private boolean ocrPersisted = false;

    @Column(name = "requester_user_id")
    private Long requesterUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
