package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.AdRewardClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdRewardClaimRepository extends JpaRepository<AdRewardClaimEntity, Long> {

    Optional<AdRewardClaimEntity> findByUserIdAndRewardId(Long userId, String rewardId);

    Optional<AdRewardClaimEntity> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
