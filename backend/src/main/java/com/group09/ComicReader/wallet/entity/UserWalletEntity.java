package com.group09.ComicReader.wallet.entity;

import com.group09.ComicReader.auth.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallets")
@Data
@ToString(exclude = {"user"})
@EqualsAndHashCode(exclude = {"user"})
public class UserWalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "coin_balance", nullable = false)
    private Integer coinBalance = 0;

    @Column(name = "point_balance", nullable = false)
    private Integer pointBalance = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
