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
@Table(name = "chapter_page_ocr_texts")
@Data
@ToString(exclude = {"chapter", "ocrJob"})
@EqualsAndHashCode(exclude = {"chapter", "ocrJob"})
public class ChapterPageOcrTextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private ChapterEntity chapter;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "source_lang", nullable = false)
    private String sourceLang;

    @Column(name = "ocr_text", nullable = false, columnDefinition = "TEXT")
    private String ocrText = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocr_job_id")
    private TranslationJobEntity ocrJob;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
