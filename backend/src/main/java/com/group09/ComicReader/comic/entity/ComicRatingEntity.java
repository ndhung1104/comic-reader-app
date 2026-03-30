package com.group09.ComicReader.comic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comic_ratings")
@Data
public class ComicRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comic_id", nullable = false)
    private Long comicId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
