package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.VipSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VipSubscriptionRepository extends JpaRepository<VipSubscriptionEntity, Long> {

    List<VipSubscriptionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT v FROM VipSubscriptionEntity v WHERE v.user.id = :userId " +
           "AND v.status = 'ACTIVE' AND v.endDate > :now ORDER BY v.endDate DESC")
    Optional<VipSubscriptionEntity> findActiveByUserId(Long userId, LocalDateTime now);
}
