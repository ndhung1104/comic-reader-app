package com.group09.ComicReader.chapter.entity;

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
@Table(name = "chapters")
@Data
@ToString(exclude = {"comic"})
@EqualsAndHashCode(exclude = {"comic"})
public class ChapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id", nullable = false)
    private ComicEntity comic;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(nullable = false)
    private String title;

    @Column(name = "language", nullable = false)
    private String language = "vi";

    @Column(nullable = false)
    private boolean premium;

    @Column(nullable = false)
    private Integer price = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "otruyen_api_data")
    private String otruyenApiData;
}
