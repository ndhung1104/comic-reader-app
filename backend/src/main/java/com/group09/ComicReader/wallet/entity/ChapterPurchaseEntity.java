package com.group09.ComicReader.wallet.entity;

import com.group09.ComicReader.auth.entity.UserEntity;
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

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_purchases")
@Data
@ToString(exclude = {"user", "chapter"})
@EqualsAndHashCode(exclude = {"user", "chapter"})
public class ChapterPurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private ChapterEntity chapter;

    @Column(name = "price_paid", nullable = false)
    private Integer pricePaid;

    @Column(nullable = false, length = 10)
    private String currency = "COIN";

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt = LocalDateTime.now();
}
