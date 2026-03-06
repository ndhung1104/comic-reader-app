package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.UserWalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWalletEntity, Long> {

    Optional<UserWalletEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM UserWalletEntity w WHERE w.user.id = :userId")
    Optional<UserWalletEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}
