package com.group09.ComicReader.translationjob.entity;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "chapter_page_tts_audios")
@Data
@ToString(exclude = {"chapter", "sourceOcrJob"})
@EqualsAndHashCode(exclude = {"chapter", "sourceOcrJob"})
public class ChapterPageTtsAudioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private ChapterEntity chapter;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "lang", nullable = false)
    private String lang;

    @Column(name = "voice", nullable = false)
    private String voice;

    @Column(name = "speed", nullable = false)
    private Double speed = 1.0;

    @Column(name = "audio_path", nullable = false, columnDefinition = "TEXT")
    private String audioPath;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "source_text_hash")
    private String sourceTextHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_ocr_job_id")
    private TranslationJobEntity sourceOcrJob;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
